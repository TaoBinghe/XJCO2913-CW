package com.greengo.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.BookingService;
import com.greengo.service.PaymentService;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private static final String BOOKING_STATUS_PENDING = "PENDING";
    private static final String BOOKING_STATUS_ACTIVE = "ACTIVE";
    private static final String BOOKING_STATUS_CANCELLED = "CANCELLED";
    private static final String SCOOTER_STATUS_AVAILABLE = "AVAILABLE";
    private static final String SCOOTER_STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String EXTENDED_PERIOD_UNAVAILABLE = "Scooter is not available for the extended period";

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ScooterMapper scooterMapper;

    @Override
    public List<PricingPlan> listPricingPlan() {
        return pricingPlanMapper.selectList(null);
    }

    @Override
    @Transactional
    public boolean bookScooter(Integer scooterId, String hiredPeriod) {
        // 1. Get pricing plan by hire period
        PricingPlan pricingPlan = findPricingPlanByHirePeriod(hiredPeriod);

        // 2. Calculate booking start and end time based on hire period
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = calculateEndTime(startTime, pricingPlan);

        // 3. Get current user id from ThreadLocal (set by LoginInterceptor)
        Long userId = currentUserId();

        // 4. One user can only have one open booking at a time.
        ensureNoOpenBooking(userId, null, "You already have an open booking");

        // 5. The scooter must still be available when the booking is created.
        assertScooterCanBeBooked(scooterId.longValue());
        reserveScooter(scooterId.longValue());
        assertPricingPlanHasPrice(pricingPlan);

        // 6. Create booking record with PENDING status (reserved, waiting for confirmation/payment)
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setScooterId(scooterId.longValue());
        booking.setPricingPlanId(pricingPlan.getId());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTotalCost(pricingPlan.getPrice());
        booking.setStatus(BOOKING_STATUS_PENDING);

        if (baseMapper.insert(booking) <= 0) {
            throw new IllegalArgumentException("Failed to create booking");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean activateBooking(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        if (!BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be activated");
        }

        ensureNoOpenBooking(booking.getUserId(), bookingId, "You already have another open booking");

        booking.setStatus(BOOKING_STATUS_ACTIVE);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to activate booking");
        }
        return true;
    }

    @Override
    @Transactional
    public Booking modifyBookingPeriod(Long bookingId, String hiredPeriod) {
        Booking booking = getOwnedBooking(bookingId);
        if (!BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can change hire period");
        }
        if (booking.getStartTime() == null) {
            throw new IllegalArgumentException("Booking start time is missing");
        }

        PricingPlan pricingPlan = findPricingPlanByHirePeriod(hiredPeriod);
        assertPricingPlanHasPrice(pricingPlan);
        booking.setPricingPlanId(pricingPlan.getId());
        booking.setEndTime(calculateEndTime(booking.getStartTime(), pricingPlan));
        booking.setTotalCost(pricingPlan.getPrice());

        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to update booking");
        }
        return booking;
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        if (BOOKING_STATUS_ACTIVE.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Active bookings cannot be cancelled; finish the ride and pay instead");
        }
        if (!BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be cancelled");
        }

        booking.setStatus(BOOKING_STATUS_CANCELLED);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to cancel booking");
        }
        releaseScooter(booking.getScooterId());
        return booking;
    }

    @Override
    @Transactional
    public Booking renewBooking(Long bookingId, String hiredPeriod) {
        Booking booking = getOwnedBooking(bookingId);
        if (!BOOKING_STATUS_ACTIVE.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only active bookings can be renewed");
        }
        if (booking.getEndTime() == null) {
            throw new IllegalArgumentException("Booking end time is missing");
        }
        if (booking.getTotalCost() == null) {
            throw new IllegalArgumentException("Booking total cost is missing");
        }

        PricingPlan pricingPlan = findPricingPlanByHirePeriod(hiredPeriod);
        assertPricingPlanHasPrice(pricingPlan);
        LocalDateTime proposedEndTime = calculateEndTime(booking.getEndTime(), pricingPlan);
        ensureScooterAvailableForRenewal(booking, proposedEndTime);
        booking.setEndTime(proposedEndTime);
        booking.setTotalCost(booking.getTotalCost().add(pricingPlan.getPrice()));

        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to renew booking");
        }
        return booking;
    }

    @Override
    public Map<String, Object> finishBooking(Long bookingId) {
        Payment payment = paymentService.pay(bookingId);
        Booking updatedBooking = baseMapper.selectById(bookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("booking", updatedBooking);
        result.put("payment", payment);
        return result;
    }

    @Override
    public List<Booking> listBookingsByUserId(Long userId) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return baseMapper.selectList(wrapper);
    }

    private Long currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return ((Number) claims.get("id")).longValue();
    }

    private Booking getOwnedBooking(Long bookingId) {
        Long userId = currentUserId();
        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        return booking;
    }

    private PricingPlan findPricingPlanByHirePeriod(String hiredPeriod) {
        PricingPlan pricingPlan = pricingPlanMapper.selectOne(
                new QueryWrapper<PricingPlan>().eq("hire_period", hiredPeriod)
        );
        if (pricingPlan == null) {
            throw new IllegalArgumentException("Pricing plan not found");
        }
        return pricingPlan;
    }

    private void assertPricingPlanHasPrice(PricingPlan pricingPlan) {
        if (pricingPlan.getPrice() == null) {
            throw new IllegalArgumentException("Pricing plan price is missing");
        }
    }

    private LocalDateTime calculateEndTime(LocalDateTime baseTime, PricingPlan pricingPlan) {
        if (baseTime == null) {
            throw new IllegalArgumentException("Booking time is missing");
        }

        switch (pricingPlan.getHirePeriod()) {
            case "HOUR_1":
                return baseTime.plusHours(1);
            case "HOUR_4":
                return baseTime.plusHours(4);
            case "DAY_1":
                return baseTime.plusDays(1);
            case "WEEK_1":
                return baseTime.plusWeeks(1);
            default:
                throw new IllegalArgumentException("Pricing plan not found");
        }
    }

    private void ensureNoOpenBooking(Long userId, Long excludeBookingId, String message) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .in("status", BOOKING_STATUS_PENDING, BOOKING_STATUS_ACTIVE);
        if (excludeBookingId != null) {
            wrapper.ne("id", excludeBookingId);
        }
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureScooterAvailableForRenewal(Booking booking, LocalDateTime proposedEndTime) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("scooter_id", booking.getScooterId())
                .ne("id", booking.getId())
                .in("status", BOOKING_STATUS_PENDING, BOOKING_STATUS_ACTIVE)
                .lt("start_time", proposedEndTime)
                .gt("end_time", booking.getEndTime());
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException(EXTENDED_PERIOD_UNAVAILABLE);
        }
    }

    private void assertScooterCanBeBooked(Long scooterId) {
        Scooter scooter = scooterMapper.selectById(scooterId);
        if (scooter == null) {
            throw new IllegalArgumentException("Scooter not found");
        }
        if (!SCOOTER_STATUS_AVAILABLE.equals(scooter.getStatus())) {
            throw new IllegalArgumentException("Scooter is not available");
        }

        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("scooter_id", scooterId)
                .in("status", BOOKING_STATUS_PENDING, BOOKING_STATUS_ACTIVE);
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Scooter is not available");
        }
    }

    private void reserveScooter(Long scooterId) {
        UpdateWrapper<Scooter> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", scooterId)
                .eq("status", SCOOTER_STATUS_AVAILABLE)
                .set("status", SCOOTER_STATUS_UNAVAILABLE);
        if (scooterMapper.update(null, wrapper) <= 0) {
            throw new IllegalArgumentException("Scooter is not available");
        }
    }

    private void releaseScooter(Long scooterId) {
        Scooter scooter = new Scooter();
        scooter.setId(scooterId);
        scooter.setStatus(SCOOTER_STATUS_AVAILABLE);
        if (scooterMapper.updateById(scooter) <= 0) {
            throw new IllegalArgumentException("Scooter not found");
        }
    }
}

