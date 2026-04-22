package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greengo.domain.BankCard;
import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.service.BankCardService;
import com.greengo.service.DistributedLockService;
import com.greengo.service.PaymentService;
import com.greengo.service.WalletService;
import com.greengo.utils.LockKeys;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.RentalConstants;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired(required = false)
    private DistributedLockService distributedLockService = new LocalDistributedLockService();

    @Autowired
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    public Payment pay(Long bookingId) {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);
        return pay(request);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    public Payment pay(PaymentRequest request) {
        if (request == null || request.getBookingId() == null || request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Booking id and payment method are required");
        }
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = ((Number) claims.get("id")).longValue();

        return distributedLockService.executeWithLock(
                LockKeys.bookingLock(request.getBookingId()),
                () -> doPay(request, userId)
        );
    }

    private Payment doPay(PaymentRequest request, Long userId) {
        Long bookingId = request.getBookingId();
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if (!RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking is not awaiting payment");
        }
        if (booking.getReturnTime() == null) {
            throw new IllegalArgumentException("Return the scooter before payment");
        }

        Long exist = paymentMapper.selectCount(new QueryWrapper<Payment>().eq("booking_id", bookingId));
        if (exist != null && exist > 0) {
            throw new IllegalArgumentException("Already paid for this booking");
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        String cardLastFour = null;
        BigDecimal amount = booking.getTotalCost() == null ? BigDecimal.ZERO : booking.getTotalCost();

        if (RentalConstants.PAYMENT_METHOD_WALLET.equals(paymentMethod)) {
            walletService.payBooking(userId, bookingId, amount);
        } else if (RentalConstants.PAYMENT_METHOD_CARD.equals(paymentMethod)) {
            if (request.getCardId() == null) {
                throw new IllegalArgumentException("Card id is required");
            }
            BankCard bankCard = bankCardService.getOwnedCard(userId, request.getCardId());
            bankCardService.verifyCardPassword(bankCard, request.getCardPassword());
            cardLastFour = bankCard.getCardLastFour();
        } else {
            throw new IllegalArgumentException("Unsupported payment method");
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus(RentalConstants.PAYMENT_STATUS_SUCCESS);
        payment.setPaymentMethod(paymentMethod);
        payment.setCardLastFour(cardLastFour);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setPaymentTime(LocalDateTime.now(clock));

        if (paymentMapper.insert(payment) <= 0) {
            throw new IllegalArgumentException("Failed to create payment");
        }

        booking.setStatus(RentalConstants.BOOKING_STATUS_COMPLETED);
        if (bookingMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to complete booking");
        }
        return payment;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        return paymentMethod.trim().toUpperCase(Locale.ROOT);
    }
}
