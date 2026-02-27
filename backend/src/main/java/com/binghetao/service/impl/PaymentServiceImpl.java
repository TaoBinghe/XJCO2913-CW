package com.binghetao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.binghetao.domain.Booking;
import com.binghetao.domain.Payment;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PaymentMapper;
import com.binghetao.service.PaymentService;
import com.binghetao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Override
    public Payment pay(Long bookingId) {
        // 1. Get current user from ThreadLocal (set by LoginInterceptor)
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId =((Number) claims.get("id")).longValue();

        // 2. Validate booking: must exist, belong to current user, and be in PENDING or ACTIVE status
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if (!"PENDING".equals(booking.getStatus()) && !"ACTIVE".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking status must be PENDING or ACTIVE");
        }

        // 3. Ensure this booking has not already been paid (one payment per booking)
        Long exist = paymentMapper.selectCount(
                new QueryWrapper<Payment>().eq("booking_id", bookingId)
        );
        if (exist != null && exist > 0) {
            throw new IllegalArgumentException("Already paid for this booking");
        }

        // 4. Simulate payment: no password or real card check, just create a successful payment record
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(booking.getTotalCost());
        payment.setStatus("SUCCESS");
        payment.setCardLastFour(null);  // Simulated payment: no real card, so last four digits are null
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setPaymentTime(LocalDateTime.now());

        // 5. Persist payment record
        paymentMapper.insert(payment);

        // 6. Update booking status
        if("SUCCESS".equals(payment.getStatus())) {
            booking.setStatus("COMPLETED");
        }else if("FAILED".equals(payment.getStatus())) {
            booking.setStatus("CANCELLED");
        }

        bookingMapper.updateById(booking);

        return payment;
    }
}
