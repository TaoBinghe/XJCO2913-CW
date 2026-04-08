package com.binghetao.service.impl;

import com.binghetao.domain.Booking;
import com.binghetao.domain.Payment;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PaymentMapper;
import com.binghetao.utils.ThreadLocalUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BookingMapper bookingMapper;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "bookingMapper", bookingMapper);
        ThreadLocalUtil.set(Map.of("id", 1L));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void payCompletesActiveBookingAndCreatesPayment() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("ACTIVE")
                .totalCost(new BigDecimal("15.00"))
                .endTime(LocalDateTime.now())
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(paymentMapper.selectCount(any())).thenReturn(0L);

        Payment payment = paymentService.pay(10L);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        verify(bookingMapper).updateById(eq(booking));
        assertEquals("COMPLETED", booking.getStatus());
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(new BigDecimal("15.00"), payment.getAmount());
        assertNotNull(payment.getTransactionId());
        assertEquals(payment, paymentCaptor.getValue());
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
    }
}
