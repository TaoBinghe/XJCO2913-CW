package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.Payment;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.PaymentService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private ScooterMapper scooterMapper;


    private com.greengo.service.impl.BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new com.greengo.service.impl.BookingServiceImpl();
        ReflectionTestUtils.setField(bookingService, "pricingPlanMapper", pricingPlanMapper);
        ReflectionTestUtils.setField(bookingService, "paymentService", paymentService);
        ReflectionTestUtils.setField(bookingService, "scooterMapper", scooterMapper);
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
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        boolean activated = bookingService.activateBooking(10L);

        assertTrue(activated);
        assertEquals("ACTIVE", booking.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void activateBookingRejectsWhenUserHasAnotherOpenBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(bookingMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.activateBooking(10L));

        assertEquals("You already have another open booking", error.getMessage());
        verify(bookingMapper, never()).updateById(booking);
    }

    @Test
    void modifyBookingPeriodUpdatesPendingBookingWithNewPlan() {
        LocalDateTime startTime = LocalDateTime.now();
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .pricingPlanId(2L)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .totalCost(new BigDecimal("5.00"))
                .status("PENDING")
                .build();
        PricingPlan newPlan = PricingPlan.builder()
                .id(3L)
                .hirePeriod("DAY_1")
                .price(new BigDecimal("30.00"))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(pricingPlanMapper.selectOne(any())).thenReturn(newPlan);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        Booking updated = bookingService.modifyBookingPeriod(10L, "DAY_1");

        assertEquals(3L, updated.getPricingPlanId());
        assertEquals(startTime.plusDays(1), updated.getEndTime());
        assertEquals(new BigDecimal("30.00"), updated.getTotalCost());
        assertEquals("PENDING", updated.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void modifyBookingPeriodRejectsActiveBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.modifyBookingPeriod(10L, "HOUR_4"));

        assertEquals("Only pending bookings can change hire period", error.getMessage());
        verify(bookingMapper, never()).updateById(booking);
    }

    @Test
    void modifyBookingPeriodRejectsBookingOwnedByAnotherUser() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(2L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.modifyBookingPeriod(10L, "HOUR_4"));

        assertEquals("Not your booking", error.getMessage());
        verify(bookingMapper, never()).updateById(any(Booking.class));
    }

    @Test
    void bookScooterRejectsWhenCurrentUserAlreadyHasOpenBooking() {
        PricingPlan plan = PricingPlan.builder()
                .id(2L)
                .hirePeriod("HOUR_1")
                .price(new BigDecimal("5.00"))
                .build();
        when(pricingPlanMapper.selectOne(any())).thenReturn(plan);
        when(bookingMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookScooter(1, "HOUR_1"));

        assertEquals("You already have an open booking", error.getMessage());
        verify(scooterMapper, never()).selectById(1L);
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    void bookScooterRejectsUnavailableScooter() {
        PricingPlan plan = PricingPlan.builder()
                .id(2L)
                .hirePeriod("HOUR_4")
                .price(new BigDecimal("15.00"))
                .build();
        Scooter scooter = Scooter.builder()
                .id(1L)
                .status("UNAVAILABLE")
                .build();
        when(pricingPlanMapper.selectOne(any())).thenReturn(plan);
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(scooterMapper.selectById(1L)).thenReturn(scooter);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookScooter(1, "HOUR_4"));

        assertEquals("Scooter is not available", error.getMessage());
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    void bookScooterReservesAvailableScooterAndCreatesPendingBooking() {
        PricingPlan plan = PricingPlan.builder()
                .id(2L)
                .hirePeriod("DAY_1")
                .price(new BigDecimal("30.00"))
                .build();
        Scooter scooter = Scooter.builder()
                .id(1L)
                .status("AVAILABLE")
                .build();
        when(pricingPlanMapper.selectOne(any())).thenReturn(plan);
        when(bookingMapper.selectCount(any())).thenReturn(0L, 0L);
        when(scooterMapper.selectById(1L)).thenReturn(scooter);
        when(scooterMapper.update(any(), any())).thenReturn(1);
        when(bookingMapper.insert(any(Booking.class))).thenReturn(1);

        boolean created = bookingService.bookScooter(1, "DAY_1");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingMapper).insert(bookingCaptor.capture());
        assertTrue(created);
        assertEquals(1L, bookingCaptor.getValue().getUserId());
        assertEquals(1L, bookingCaptor.getValue().getScooterId());
        assertEquals(2L, bookingCaptor.getValue().getPricingPlanId());
        assertEquals(new BigDecimal("30.00"), bookingCaptor.getValue().getTotalCost());
        assertEquals("PENDING", bookingCaptor.getValue().getStatus());
        assertNotNull(bookingCaptor.getValue().getStartTime());
        assertTrue(bookingCaptor.getValue().getEndTime().isAfter(bookingCaptor.getValue().getStartTime()));
        verify(scooterMapper).update(any(), any());
    }

    @Test
    void cancelBookingCancelsPendingBookingOwnedByCurrentUserAndReleasesScooter() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .scooterId(5L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(bookingMapper.updateById(booking)).thenReturn(1);
        when(scooterMapper.updateById(any(Scooter.class))).thenReturn(1);

        Booking cancelled = bookingService.cancelBooking(10L);

        ArgumentCaptor<Scooter> scooterCaptor = ArgumentCaptor.forClass(Scooter.class);
        assertEquals("CANCELLED", cancelled.getStatus());
        verify(bookingMapper).updateById(booking);
        verify(scooterMapper).updateById(scooterCaptor.capture());
        assertEquals(5L, scooterCaptor.getValue().getId());
        assertEquals("AVAILABLE", scooterCaptor.getValue().getStatus());
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
    void renewBookingExtendsActiveBookingAndKeepsOriginalPlan() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusHours(4);
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .pricingPlanId(2L)
                .endTime(originalEndTime)
                .totalCost(new BigDecimal("15.00"))
                .status("ACTIVE")
                .build();
        PricingPlan renewalPlan = PricingPlan.builder()
                .id(4L)
                .hirePeriod("HOUR_1")
                .price(new BigDecimal("5.00"))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(pricingPlanMapper.selectOne(any())).thenReturn(renewalPlan);
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        Booking updated = bookingService.renewBooking(10L, "HOUR_1");

        assertEquals(2L, updated.getPricingPlanId());
        assertEquals(originalEndTime.plusHours(1), updated.getEndTime());
        assertEquals(new BigDecimal("20.00"), updated.getTotalCost());
        assertEquals("ACTIVE", updated.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void renewBookingRejectsWhenExtendedPeriodOverlapsAnotherOpenBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .scooterId(3L)
                .endTime(LocalDateTime.now().plusHours(4))
                .totalCost(new BigDecimal("15.00"))
                .status("ACTIVE")
                .build();
        PricingPlan renewalPlan = PricingPlan.builder()
                .id(4L)
                .hirePeriod("HOUR_4")
                .price(new BigDecimal("15.00"))
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(pricingPlanMapper.selectOne(any())).thenReturn(renewalPlan);
        when(bookingMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.renewBooking(10L, "HOUR_4"));

        assertEquals("Scooter is not available for the extended period", error.getMessage());
        verify(bookingMapper, never()).updateById(any(Booking.class));
    }

    @Test
    void renewBookingRejectsPendingBooking() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .status("PENDING")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.renewBooking(10L, "HOUR_1"));

        assertEquals("Only active bookings can be renewed", error.getMessage());
        verify(bookingMapper, never()).updateById(booking);
    }

    @Test
    void renewBookingRejectsUnknownPricingPlan() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .endTime(LocalDateTime.now().plusHours(1))
                .totalCost(new BigDecimal("5.00"))
                .status("ACTIVE")
                .build();
        when(bookingMapper.selectById(10L)).thenReturn(booking);
        when(pricingPlanMapper.selectOne(any())).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.renewBooking(10L, "UNKNOWN"));

        assertEquals("Pricing plan not found", error.getMessage());
        verify(bookingMapper, never()).updateById(any(Booking.class));
    }

    @Test
    void finishBookingReturnsUpdatedBookingAndPayment() {
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

        when(bookingMapper.selectById(10L)).thenReturn(completedBooking);
        when(paymentService.pay(10L)).thenReturn(payment);

        Map<String, Object> result = bookingService.finishBooking(10L);

        verify(paymentService).pay(10L);
        assertEquals(completedBooking, result.get("booking"));
        assertEquals(payment, result.get("payment"));
    }

    @Test
    void finishBookingPropagatesPaymentErrors() {
        when(paymentService.pay(10L)).thenThrow(new IllegalArgumentException("Not your booking"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.finishBooking(10L));

        assertEquals("Not your booking", error.getMessage());
        verify(paymentService).pay(10L);
    }
}

