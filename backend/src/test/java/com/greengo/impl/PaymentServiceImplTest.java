package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PaymentMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.DistributedLockService;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ScooterMapper scooterMapper;

    @Mock
    private DistributedLockService distributedLockService;

    private com.greengo.service.impl.PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new com.greengo.service.impl.PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "bookingMapper", bookingMapper);
        ReflectionTestUtils.setField(paymentService, "scooterMapper", scooterMapper);
        ReflectionTestUtils.setField(paymentService, "distributedLockService", distributedLockService);
        ThreadLocalUtil.set(Map.of("id", 1L));

        lenient().when(distributedLockService.executeWithLock(any(String.class), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void payCompletesActiveBookingAndCreatesPayment() {
        LocalDateTime plannedEnd = LocalDateTime.now().plusHours(2);
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .scooterId(3L)
                .status("ACTIVE")
                .totalCost(new BigDecimal("15.00"))
                .endTime(plannedEnd)
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(0L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(bookingMapper.updateById(eq(booking))).thenReturn(1);
        when(scooterMapper.updateById(any(Scooter.class))).thenReturn(1);

        Payment payment = paymentService.pay(10L);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        ArgumentCaptor<Scooter> scooterCaptor = ArgumentCaptor.forClass(Scooter.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        verify(bookingMapper).updateById(eq(booking));
        verify(scooterMapper).updateById(scooterCaptor.capture());
        assertEquals("COMPLETED", booking.getStatus());
        assertNotNull(booking.getEndTime());
        assertTrue(booking.getEndTime().isBefore(plannedEnd));
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(new BigDecimal("15.00"), payment.getAmount());
        assertNotNull(payment.getTransactionId());
        assertEquals(payment, paymentCaptor.getValue());
        assertEquals(3L, scooterCaptor.getValue().getId());
        assertEquals("AVAILABLE", scooterCaptor.getValue().getStatus());
    }

    @Test
    void payRejectsPendingBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Booking status must be ACTIVE", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }

    @Test
    void payRejectsCompletedBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("COMPLETED")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Booking status must be ACTIVE", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }

    @Test
    void payRejectsBookingOwnedByAnotherUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(2L)
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Not your booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }

    @Test
    void payRejectsAlreadyPaidBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay(10L));

        assertEquals("Already paid for this booking", error.getMessage());
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }
}

