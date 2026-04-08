package com.binghetao.service.impl;

import com.binghetao.domain.Booking;
import com.binghetao.domain.Payment;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PricingPlanMapper;
import com.binghetao.service.PaymentService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PricingPlanMapper pricingPlanMapper;

    @Mock
    private PaymentService paymentService;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl();
        ReflectionTestUtils.setField(bookingService, "pricingPlanMapper", pricingPlanMapper);
        ReflectionTestUtils.setField(bookingService, "paymentService", paymentService);
        ReflectionTestUtils.setField(bookingService, "baseMapper", bookingMapper);
        ThreadLocalUtil.set(Map.of("id", 1L));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void activateBookingMarksPendingBookingAsActive() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        boolean activated = bookingService.activateBooking(10L);

        assertTrue(activated);
        assertEquals("ACTIVE", booking.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void cancelBookingCancelsPendingBookingOwnedByCurrentUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        Booking cancelled = bookingService.cancelBooking(10L);

        assertEquals("CANCELLED", cancelled.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void cancelBookingRejectsActiveBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelBooking(10L));

        assertEquals("Active bookings cannot be cancelled; finish the ride and pay instead", error.getMessage());
        verify(bookingMapper, never()).updateById(booking);
    }

    @Test
    void finishBookingUpdatesEndTimeAndReturnsBookingAndPayment() {
        LocalDateTime plannedEnd = LocalDateTime.now().plusHours(2);
        Booking activeBooking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("ACTIVE")
                .endTime(plannedEnd)
                .build();
        Booking completedBooking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("COMPLETED")
                .endTime(LocalDateTime.now())
                .build();
        Payment payment = Payment.builder()
                .bookingId(10L)
                .userId(1L)
                .amount(new BigDecimal("15.00"))
                .status("SUCCESS")
                .transactionId("TXN-1234567890")
                .paymentTime(LocalDateTime.now())
                .build();

        when(bookingMapper.selectById(10L)).thenReturn(activeBooking, completedBooking);
        when(paymentService.pay(10L)).thenReturn(payment);

        Map<String, Object> result = bookingService.finishBooking(10L);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingMapper).updateById(bookingCaptor.capture());
        assertNotNull(bookingCaptor.getValue().getEndTime());
        assertTrue(bookingCaptor.getValue().getEndTime().isBefore(plannedEnd));
        assertEquals(completedBooking, result.get("booking"));
        assertEquals(payment, result.get("payment"));
    }

    @Test
    void finishBookingRejectsBookingOwnedByAnotherUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(2L)
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.finishBooking(10L));

        assertEquals("Not your booking", error.getMessage());
        verify(paymentService, never()).pay(10L);
    }
}
