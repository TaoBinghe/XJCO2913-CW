package com.binghetao.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.binghetao.domain.Booking;
import com.binghetao.domain.Payment;
import com.binghetao.domain.PricingPlan;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PricingPlanMapper;
import com.binghetao.service.BookingService;
import com.binghetao.service.PaymentService;
import com.binghetao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Autowired
    private PaymentService paymentService;

    @Override
    public List<PricingPlan> listPricingPlan() {
        return pricingPlanMapper.selectList(null);
    }

    @Override
    public boolean bookScooter(Integer scooterId, String hiredPeriod) {
        // 1. Get pricing plan by hire period
        PricingPlan pricingPlan = pricingPlanMapper.selectOne(
                new QueryWrapper<PricingPlan>().eq("hire_period", hiredPeriod)
        );
        if (pricingPlan == null) {
            return false;
        }

        // 2. Calculate booking start and end time based on hire period
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        switch (pricingPlan.getHirePeriod()) {
            case "HOUR_1":
                endTime = startTime.plusHours(1);
                break;
            case "HOUR_4":
                endTime = startTime.plusHours(4);
                break;
            case "DAY_1":
                endTime = startTime.plusDays(1);
                break;
            case "WEEK_1":
                endTime = startTime.plusWeeks(1);
                break;
            default:
                return false;
        }

        // 3. Check if the scooter is already booked in the requested period
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("scooter_id", scooterId)
                .in("status", "PENDING", "ACTIVE")
                .lt("start_time", endTime)
                .gt("end_time", startTime);
        Long clashCount = baseMapper.selectCount(wrapper);
        if (clashCount != null && clashCount > 0) {
            return false;
        }

        // 4. Get current user id from ThreadLocal (set by LoginInterceptor)
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        // 5. Create booking record with PENDING status (reserved, waiting for confirmation/payment)
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setScooterId(scooterId.longValue());
        booking.setPricingPlanId(pricingPlan.getId());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTotalCost(pricingPlan.getPrice());
        booking.setStatus("PENDING");

        baseMapper.insert(booking);
        return true;
    }

    @Override
    public boolean activateBooking(Long bookingId) {
        // Get current user
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        // Load booking
        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            return false;
        }

        // Only owner can activate and only when status is PENDING
        if (!booking.getUserId().equals(userId)) {
            return false;
        }
        if (!"PENDING".equals(booking.getStatus())) {
            return false;
        }

        booking.setStatus("ACTIVE");
        baseMapper.updateById(booking);
        return true;
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if ("ACTIVE".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Active bookings cannot be cancelled; finish the ride and pay instead");
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        baseMapper.updateById(booking);
        return booking;
    }

    @Override
    @Transactional
    public Map<String, Object> finishBooking(Long bookingId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if (!"ACTIVE".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only active bookings can be finished");
        }

        booking.setEndTime(LocalDateTime.now());
        baseMapper.updateById(booking);

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
}
