package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.service.DistributedLockService;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    private DistributedLockService distributedLockService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "bookingMapper", bookingMapper);
        ReflectionTestUtils.setField(paymentService, "distributedLockService", distributedLockService);
        ReflectionTestUtils.setField(paymentService, "clock", FIXED_CLOCK);
        ThreadLocalUtil.set(Map.of("id", 1L));

        lenient().when(distributedLockService.executeWithLock(any(String.class), org.mockito.ArgumentMatchers.<Supplier<?>>any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void payCreatesPaymentForReturnedBookingAndCompletesIt() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_IN_PROGRESS)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .totalCost(new BigDecimal("35.00"))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(0L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        Payment payment = paymentService.pay(10L);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertEquals(RentalConstants.BOOKING_STATUS_COMPLETED, booking.getStatus());
        assertEquals(new BigDecimal("35.00"), payment.getAmount());
        assertEquals(LocalDateTime.of(2026, 4, 15, 10, 0), payment.getPaymentTime());
        assertNotNull(payment.getTransactionId());
        assertEquals(payment, paymentCaptor.getValue());
    }

    @Test
    void payRejectsBookingOwnedByAnotherUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(2L)
                .status(RentalConstants.BOOKING_STATUS_IN_PROGRESS)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Not your booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsBookingThatHasNotBeenReturnedYet() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_IN_PROGRESS)
                .returnTime(null)
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Return the scooter before payment", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsAlreadyPaidBookingAfterReturn() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_OVERDUE)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Already paid for this booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payRejectsBookingOutsideReturnSettlementStatuses() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status(RentalConstants.BOOKING_STATUS_COMPLETED)
                .returnTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Booking must be returned before payment", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }
}
