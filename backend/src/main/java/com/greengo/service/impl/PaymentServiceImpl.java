package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greengo.domain.BankCard;
import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;
import com.greengo.domain.User;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.mapper.UserMapper;
import com.greengo.service.BankCardService;
import com.greengo.service.PaymentService;
import com.greengo.service.WalletService;
import com.greengo.utils.RentalConstants;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final long FREQUENT_USER_MINUTES_THRESHOLD = 8L * 60L;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    public Payment pay(PaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new IllegalArgumentException("Booking id is required");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Long userId = ((Number) claims.get("id")).longValue();

        Booking booking = loadBookingForUpdate(request.getBookingId());
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        if (booking.getReturnTime() == null) {
            throw new IllegalArgumentException("Return the scooter before payment");
        }
        if (!RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking is not awaiting payment");
        }

        Long exist = paymentMapper.selectCount(new QueryWrapper<Payment>().eq("booking_id", booking.getId()));
        if (exist != null && exist > 0) {
            throw new IllegalArgumentException("Already paid for this booking");
        }

        LocalDateTime paymentTime = LocalDateTime.now(clock);
        BigDecimal originalAmount = normalizeMoney(booking.getTotalCost() == null ? BigDecimal.ZERO : booking.getTotalCost());
        DiscountResult discount = calculateDiscount(loadUser(userId), userId, originalAmount, paymentTime);
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        String cardLastFour = handlePaymentMethod(paymentMethod, userId, booking.getId(), discount.payableAmount(), request);

        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .userId(userId)
                .originalAmount(originalAmount)
                .discountType(discount.discountType())
                .discountRate(discount.discountRate())
                .discountAmount(discount.discountAmount())
                .amount(discount.payableAmount())
                .status(RentalConstants.PAYMENT_STATUS_SUCCESS)
                .paymentMethod(paymentMethod)
                .cardLastFour(cardLastFour)
                .transactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .paymentTime(paymentTime)
                .build();

        try {
            if (paymentMapper.insert(payment) <= 0) {
                throw new IllegalArgumentException("Failed to create payment");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Already paid for this booking");
        }

        booking.setStatus(RentalConstants.BOOKING_STATUS_COMPLETED);
        if (bookingMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to complete booking");
        }
        return payment;
    }

    private DiscountResult calculateDiscount(User user,
                                             Long userId,
                                             BigDecimal originalAmount,
                                             LocalDateTime paymentTime) {
        if (user == null) {
            return new DiscountResult(RentalConstants.DISCOUNT_TYPE_NONE, BigDecimal.ZERO, BigDecimal.ZERO, originalAmount);
        }
        String discountType = resolveDiscountType(user, userId, paymentTime);
        BigDecimal discountRate = RentalConstants.DISCOUNT_TYPE_NONE.equals(discountType)
                ? BigDecimal.ZERO
                : DISCOUNT_RATE;
        BigDecimal discountAmount = normalizeMoney(originalAmount.multiply(discountRate));
        BigDecimal payableAmount = normalizeMoney(originalAmount.subtract(discountAmount));
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new DiscountResult(discountType, discountRate, discountAmount, payableAmount);
    }

    private Booking loadBookingForUpdate(Long bookingId) {
        if (bookingMapper == null) {
            return null;
        }
        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            booking = bookingMapper.selectById(bookingId);
        }
        return booking;
    }

    private User loadUser(Long userId) {
        if (userMapper == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return user;
    }

    private String resolveDiscountType(User user, Long userId, LocalDateTime paymentTime) {
        String customerType = normalizeCustomerType(user.getCustomerType());
        if (RentalConstants.CUSTOMER_TYPE_STUDENT.equals(customerType)) {
            return RentalConstants.DISCOUNT_TYPE_STUDENT;
        }
        if (RentalConstants.CUSTOMER_TYPE_SENIOR.equals(customerType)) {
            return RentalConstants.DISCOUNT_TYPE_SENIOR;
        }

        LocalDateTime windowStart = paymentTime.minusDays(7);
        Long paidMinutes = paymentMapper.selectPaidRentalMinutes(userId, windowStart, paymentTime);
        if (paidMinutes != null && paidMinutes >= FREQUENT_USER_MINUTES_THRESHOLD) {
            return RentalConstants.DISCOUNT_TYPE_FREQUENT_USER;
        }
        return RentalConstants.DISCOUNT_TYPE_NONE;
    }

    private String normalizeCustomerType(String customerType) {
        if (customerType == null || customerType.isBlank()) {
            return RentalConstants.CUSTOMER_TYPE_REGULAR;
        }
        return customerType.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method is required");
        }

        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!RentalConstants.PAYMENT_METHOD_WALLET.equals(normalized)
                && !RentalConstants.PAYMENT_METHOD_CARD.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported payment method");
        }
        return normalized;
    }

    private String handlePaymentMethod(String paymentMethod,
                                       Long userId,
                                       Long bookingId,
                                       BigDecimal amount,
                                       PaymentRequest request) {
        if (RentalConstants.PAYMENT_METHOD_WALLET.equals(paymentMethod)) {
            walletService.payBooking(userId, bookingId, amount);
            return null;
        }

        BankCard bankCard = bankCardService.getOwnedCard(userId, request.getCardId());
        bankCardService.verifyCardPassword(bankCard, request.getCardPassword());
        return bankCard.getCardLastFour();
    }

    private record DiscountResult(String discountType,
                                  BigDecimal discountRate,
                                  BigDecimal discountAmount,
                                  BigDecimal payableAmount) {
    }
}
