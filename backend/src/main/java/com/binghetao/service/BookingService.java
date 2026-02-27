package com.binghetao.service;

import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BookingService {
    List<PricingPlan> listPricingPlan();

    boolean bookScooter(Integer scooterId, String hiredPeriod);

    /**
     * Activate a booking after the user confirms the selection on frontend.
     *
     * @param bookingId the booking to activate
     * @return true if the booking is successfully activated
     */
    boolean activateBooking(Long bookingId);

    /**
     * 查询指定用户的历史订单（预订记录），按创建时间倒序。
     *
     * @param userId 用户 ID
     * @return 该用户的预订列表
     */
    List<Booking> listBookingsByUserId(Long userId);
}
