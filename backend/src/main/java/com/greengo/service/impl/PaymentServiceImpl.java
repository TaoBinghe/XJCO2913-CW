package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.DistributedLockService;
import com.greengo.service.PaymentService;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.RedisKeys;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String BOOKING_STATUS_ACTIVE = "ACTIVE";
    private static final String BOOKING_STATUS_COMPLETED = "COMPLETED";
    private static final String SCOOTER_STATUS_AVAILABLE = "AVAILABLE";

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true),
            @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    })
    public Payment pay(Long bookingId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        return distributedLockService.executeWithLock(RedisKeys.bookingLock(bookingId), () -> {
            Booking booking = bookingMapper.selectById(bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Booking not found");
            }
            return distributedLockService.executeWithLock(
                    RedisKeys.scooterLock(booking.getScooterId()),
                    () -> doPay(bookingId, userId)
            );
        });
    }

    private Payment doPay(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if (!BOOKING_STATUS_ACTIVE.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking status must be ACTIVE");
        }

        Long exist = paymentMapper.selectCount(
                new QueryWrapper<Payment>().eq("booking_id", bookingId)
        );
        if (exist != null && exist > 0) {
            throw new IllegalArgumentException("Already paid for this booking");
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(booking.getTotalCost());
        payment.setStatus("SUCCESS");
        payment.setCardLastFour(null);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setPaymentTime(LocalDateTime.now());

        if (paymentMapper.insert(payment) <= 0) {
            throw new IllegalArgumentException("Failed to create payment");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            booking.setEndTime(LocalDateTime.now());
            booking.setStatus(BOOKING_STATUS_COMPLETED);
            if (bookingMapper.updateById(booking) <= 0) {
                throw new IllegalArgumentException("Failed to complete booking");
            }

            Scooter scooter = new Scooter();
            scooter.setId(booking.getScooterId());
            scooter.setStatus(SCOOTER_STATUS_AVAILABLE);
            if (scooterMapper.updateById(scooter) <= 0) {
                throw new IllegalArgumentException("Scooter not found");
            }
        }

        return payment;
    }
}
