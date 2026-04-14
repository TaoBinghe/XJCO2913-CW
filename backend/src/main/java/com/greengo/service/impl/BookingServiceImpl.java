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
import com.greengo.service.DistributedLockService;
import com.greengo.service.PaymentService;
import com.greengo.utils.PricingPlanPeriodUtil;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.RedisKeys;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    @Cacheable(value = RedisCacheNames.PRICING_PLAN_LIST, key = "'all'")
    public List<PricingPlan> listPricingPlan() {
        return pricingPlanMapper.selectList(null);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean bookScooter(Integer scooterId, String hiredPeriod) {
        Long userId = currentUserId();
        return distributedLockService.executeWithLocks(
                List.of(RedisKeys.userBookingLock(userId), RedisKeys.scooterLock(scooterId.longValue())),
                () -> doBookScooter(userId, scooterId.longValue(), hiredPeriod)
        );
    }

    @Override
    @Transactional
    public boolean activateBooking(Long bookingId) {
        Long userId = currentUserId();
        return distributedLockService.executeWithLocks(
                List.of(RedisKeys.userBookingLock(userId), RedisKeys.bookingLock(bookingId)),
                () -> doActivateBooking(bookingId)
        );
    }

    @Override
    @Transactional
    public boolean updateBookingStatus(Long bookingId, String status) {
        String targetStatus = normalizeLegacyBookingStatus(status);
        if (BOOKING_STATUS_ACTIVE.equals(targetStatus)) {
            return activateBooking(bookingId);
        }
        return distributedLockService.executeWithLock(
                RedisKeys.bookingLock(bookingId),
                () -> doUpdateBookingStatus(bookingId, targetStatus)
        );
    }

    @Override
    @Transactional
    public Booking modifyBookingPeriod(Long bookingId, String hiredPeriod) {
        return distributedLockService.executeWithLock(
                RedisKeys.bookingLock(bookingId),
                () -> doModifyBookingPeriod(bookingId, hiredPeriod)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public Booking cancelBooking(Long bookingId) {
        return distributedLockService.executeWithLock(RedisKeys.bookingLock(bookingId), () -> {
            Booking booking = getOwnedBooking(bookingId);
            return distributedLockService.executeWithLock(
                    RedisKeys.scooterLock(booking.getScooterId()),
                    () -> doCancelBooking(booking)
            );
        });
    }

    @Override
    @Transactional
    public Booking renewBooking(Long bookingId, String hiredPeriod) {
        return distributedLockService.executeWithLock(
                RedisKeys.bookingLock(bookingId),
                () -> doRenewBooking(bookingId, hiredPeriod)
        );
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

    private boolean doBookScooter(Long userId, Long scooterId, String hiredPeriod) {
        PricingPlan pricingPlan = findPricingPlanByHirePeriod(hiredPeriod);
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = calculateEndTime(startTime, pricingPlan);

        ensureNoOpenBooking(userId, null, "You already have an open booking");
        assertScooterCanBeBooked(scooterId);
        reserveScooter(scooterId);
        assertPricingPlanHasPrice(pricingPlan);

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setScooterId(scooterId);
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

    private boolean doActivateBooking(Long bookingId) {
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

    private boolean doUpdateBookingStatus(Long bookingId, String targetStatus) {
        Booking booking = getOwnedBooking(bookingId);
        if (targetStatus.equals(booking.getStatus())) {
            return true;
        }
        if (!BOOKING_STATUS_ACTIVE.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only active bookings can be switched back to pending");
        }

        booking.setStatus(BOOKING_STATUS_PENDING);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to update booking status");
        }
        return true;
    }

    private Booking doModifyBookingPeriod(Long bookingId, String hiredPeriod) {
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

    private Booking doCancelBooking(Booking booking) {
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

    private Booking doRenewBooking(Long bookingId, String hiredPeriod) {
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
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        return booking;
    }

    private PricingPlan findPricingPlanByHirePeriod(String hiredPeriod) {
        String normalizedHirePeriod = PricingPlanPeriodUtil.normalizeHirePeriod(hiredPeriod);
        if (normalizedHirePeriod == null) {
            throw new IllegalArgumentException("Pricing plan not found");
        }

        PricingPlan pricingPlan = pricingPlanMapper.selectOne(
                new QueryWrapper<PricingPlan>().eq("hire_period", normalizedHirePeriod)
        );
        if (pricingPlan == null) {
            throw new IllegalArgumentException("Pricing plan not found");
        }
        return pricingPlan;
    }

    private String normalizeLegacyBookingStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Booking status is missing");
        }
        if ("ACTIVATED".equalsIgnoreCase(status) || BOOKING_STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return BOOKING_STATUS_ACTIVE;
        }
        if (BOOKING_STATUS_PENDING.equalsIgnoreCase(status)) {
            return BOOKING_STATUS_PENDING;
        }
        throw new IllegalArgumentException("Unsupported booking status");
    }

    private void assertPricingPlanHasPrice(PricingPlan pricingPlan) {
        if (pricingPlan.getPrice() == null) {
            throw new IllegalArgumentException("Pricing plan price is missing");
        }
    }

    private LocalDateTime calculateEndTime(LocalDateTime baseTime, PricingPlan pricingPlan) {
        return PricingPlanPeriodUtil.addPeriod(baseTime, pricingPlan.getHirePeriod());
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
