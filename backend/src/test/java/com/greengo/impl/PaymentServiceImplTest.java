package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.BankCard;
import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.service.BankCardService;
import com.greengo.service.WalletService;
import com.greengo.service.impl.PaymentServiceImpl;
import com.greengo.utils.RentalConstants;
import com.greengo.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-15T02:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private BankCardService bankCardService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "bookingMapper", bookingMapper);
        ReflectionTestUtils.setField(paymentService, "walletService", walletService);
        ReflectionTestUtils.setField(paymentService, "bankCardService", bankCardService);
        ReflectionTestUtils.setField(paymentService, "clock", FIXED_CLOCK);
        ThreadLocalUtil.set(Map.of("id", 1L));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void walletPayCreatesPaymentForAwaitingBookingAndCompletesIt() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .totalCost(new BigDecimal("35.00"))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(0L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);

        Payment payment = paymentService.pay(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        verify(walletService).payBooking(1L, 10L, new BigDecimal("35.00"));
        assertEquals(RentalConstants.BOOKING_STATUS_COMPLETED, booking.getStatus());
        assertEquals(new BigDecimal("35.00"), payment.getAmount());
        assertEquals(RentalConstants.PAYMENT_METHOD_WALLET, payment.getPaymentMethod());
        assertEquals(LocalDateTime.of(2026, 4, 15, 10, 0), payment.getPaymentTime());
        assertNotNull(payment.getTransactionId());
        assertEquals(payment, paymentCaptor.getValue());
    }

    @Test
    void cardPayStoresCardLastFourForBoundCard() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .totalCost(new BigDecimal("35.00"))
                .build();
        BankCard bankCard = BankCard.builder()
                .id(99L)
                .userId(1L)
                .cardLastFour("1234")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(0L);
        when(bankCardService.getOwnedCard(1L, 99L)).thenReturn(bankCard);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_CARD);
        request.setCardId(99L);
        request.setCardPassword("123456");

        Payment payment = paymentService.pay(request);

        verify(bankCardService).verifyCardPassword(bankCard, "123456");
        verify(walletService, never()).payBooking(any(), any(), any());
        assertEquals(RentalConstants.PAYMENT_METHOD_CARD, payment.getPaymentMethod());
        assertEquals("1234", payment.getCardLastFour());
        assertEquals(RentalConstants.BOOKING_STATUS_COMPLETED, booking.getStatus());
    }

    @Test
    void payRejectsBookingOwnedByAnotherUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(2L)
                .status(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(request));

        assertEquals("Not your booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsBookingThatHasNotBeenReturnedYet() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT)
                .returnTime(null)
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(request));

        assertEquals("Return the scooter before payment", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsAlreadyPaidBookingAfterReturn() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(1L);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(request));

        assertEquals("Already paid for this booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsBookingOutsideAwaitingPaymentStatus() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_COMPLETED)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(10L);
        request.setPaymentMethod(RentalConstants.PAYMENT_METHOD_WALLET);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(request));

        assertEquals("Booking is not awaiting payment", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }
}
