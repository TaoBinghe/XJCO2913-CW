package com.binghetao.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PricingPlanMapper;
import com.binghetao.service.BookingService;
import com.binghetao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVATED = "ACTIVATED";
    private static final String LEGACY_STATUS_ACTIVE = "ACTIVE";

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

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
                .in("status", STATUS_PENDING, STATUS_ACTIVATED, LEGACY_STATUS_ACTIVE)
                .lt("start_time", endTime)
                .gt("end_time", startTime);
        Long clashCount = baseMapper.selectCount(wrapper);
        if (clashCount != null && clashCount > 0) {
            return false;
        }

        // 4. Get current user id from ThreadLocal (set by LoginInterceptor)
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        // 5. Create booking record as PENDING; user can activate it manually later
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setScooterId(scooterId.longValue());
        booking.setPricingPlanId(pricingPlan.getId());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTotalCost(pricingPlan.getPrice());
        booking.setStatus(STATUS_PENDING);

        baseMapper.insert(booking);
        return true;
    }

    @Override
    public boolean updateBookingStatus(Long bookingId, String status) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            return false;
        }
        if (!booking.getUserId().equals(userId)) {
            return false;
        }

        String currentStatus = normalizeStatus(booking.getStatus());
        String targetStatus = normalizeStatus(status);

        if (!STATUS_PENDING.equals(currentStatus) && !STATUS_ACTIVATED.equals(currentStatus)) {
            return false;
        }
        if (!STATUS_PENDING.equals(targetStatus) && !STATUS_ACTIVATED.equals(targetStatus)) {
            return false;
        }

        if (currentStatus.equals(targetStatus)) {
            if (!targetStatus.equals(booking.getStatus())) {
                booking.setStatus(targetStatus);
                baseMapper.updateById(booking);
            }
            return true;
        }

        booking.setStatus(targetStatus);
        baseMapper.updateById(booking);
        return true;
    }

    @Override
    public boolean activateBooking(Long bookingId) {
        return updateBookingStatus(bookingId, STATUS_ACTIVATED);
    }

    @Override
    public List<Booking> listBookingsByUserId(Long userId) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return baseMapper.selectList(wrapper);
    }

    private String normalizeStatus(String status) {
        if (LEGACY_STATUS_ACTIVE.equals(status)) {
            return STATUS_ACTIVATED;
        }
        return status;
    }
}
