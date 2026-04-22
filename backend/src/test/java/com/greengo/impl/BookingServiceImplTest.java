package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.BookingSettlementResult;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.GeoAddressService;
import com.greengo.service.impl.BookingServiceImpl;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long STORE_ID = 7L;
    private static final Long BOOKING_ID = 10L;
    private static final Long SCOOTER_ID = 3L;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-15T02:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PricingPlanMapper pricingPlanMapper;

    @Mock
    private ScooterMapper scooterMapper;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private GeoAddressService geoAddressService;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl();
        ReflectionTestUtils.setField(bookingService, "pricingPlanMapper", pricingPlanMapper);
        ReflectionTestUtils.setField(bookingService, "scooterMapper", scooterMapper);
        ReflectionTestUtils.setField(bookingService, "storeMapper", storeMapper);
        ReflectionTestUtils.setField(bookingService, "geoAddressService", geoAddressService);
        ReflectionTestUtils.setField(bookingService, "baseMapper", bookingMapper);
        ReflectionTestUtils.setField(bookingService, "clock", FIXED_CLOCK);
        ThreadLocalUtil.set(Map.of("id", USER_ID));

        lenient().when(storeMapper.selectBatchIds(any())).thenReturn(List.of());
        lenient().when(scooterMapper.selectBatchIds(any())).thenReturn(List.of());
        lenient().when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void createStoreBookingCreatesReservedBookingForStoreInventory() {
        Store store = enabledStore();
        PricingPlan dayPlan = pricingPlan(2L, "DAY_1", "30.00");
        LocalDateTime appointmentStart = LocalDateTime.of(2026, 4, 16, 10, 0);
        AtomicReference<Booking> storedBooking = new AtomicReference<>();

        when(storeMapper.selectById(STORE_ID)).thenReturn(store);
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));
        when(pricingPlanMapper.selectOne(any())).thenReturn(dayPlan);
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(dayPlan));
        when(scooterMapper.selectCount(any())).thenReturn(3L);
        when(bookingMapper.selectCount(any())).thenReturn(0L, 0L);
        when(bookingMapper.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(BOOKING_ID);
            storedBooking.set(booking);
            return 1;
        });
        when(bookingMapper.selectById(BOOKING_ID)).thenAnswer(invocation -> storedBooking.get());

        Booking booking = bookingService.createStoreBooking(STORE_ID, appointmentStart, "DAY_1");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingMapper).insert(bookingCaptor.capture());
        assertEquals(RentalConstants.BOOKING_STATUS_RESERVED, bookingCaptor.getValue().getStatus());
        assertEquals(RentalConstants.RENTAL_TYPE_STORE_PICKUP, bookingCaptor.getValue().getRentalType());
        assertEquals(appointmentStart.plusDays(1), bookingCaptor.getValue().getEndTime());
        assertEquals(appointmentStart.plusMinutes(30), bookingCaptor.getValue().getPickupDeadline());
        assertEquals(new BigDecimal("30.00"), bookingCaptor.getValue().getTotalCost());
        assertEquals(STORE_ID, booking.getStoreId());
        assertEquals("Xipu North Hub", booking.getStoreName());
        assertEquals("DAY_1", booking.getHirePeriod());
    }

    @Test
    void createStoreBookingRejectsWhenStoreInventoryIsFullyBooked() {
        Store store = enabledStore();
        PricingPlan dayPlan = pricingPlan(2L, "DAY_1", "30.00");
        LocalDateTime appointmentStart = LocalDateTime.of(2026, 4, 16, 10, 0);

        when(storeMapper.selectById(STORE_ID)).thenReturn(store);
        when(pricingPlanMapper.selectOne(any())).thenReturn(dayPlan);
        when(scooterMapper.selectCount(any())).thenReturn(2L);
        when(bookingMapper.selectCount(any())).thenReturn(0L, 2L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createStoreBooking(STORE_ID, appointmentStart, "DAY_1"));

        assertEquals("Store inventory is fully booked for the selected time", error.getMessage());
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    void cancelStoreBookingMarksReservedBookingCancelled() {
        Store store = enabledStore();
        PricingPlan dayPlan = pricingPlan(2L, "DAY_1", "30.00");
        Booking booking = reservedBooking();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(bookingMapper.updateById(booking)).thenReturn(1);
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(dayPlan));

        Booking cancelled = bookingService.cancelStoreBooking(BOOKING_ID);

        assertEquals(RentalConstants.BOOKING_STATUS_CANCELLED, cancelled.getStatus());
        verify(bookingMapper).updateById(booking);
    }

    @Test
    void startScanRideCreatesInProgressBookingAndUnlocksScooter() {
        Scooter scooter = scanRideScooter();
        PricingPlan minutePlan = pricingPlan(9L, "MINUTE_1", "1.00");
        AtomicReference<Booking> storedBooking = new AtomicReference<>();

        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(scooterMapper.selectOne(any())).thenReturn(scooter);
        when(bookingMapper.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(BOOKING_ID);
            storedBooking.set(booking);
            return 1;
        });
        when(bookingMapper.selectById(BOOKING_ID)).thenAnswer(invocation -> storedBooking.get());
        when(pricingPlanMapper.selectOne(any())).thenReturn(minutePlan);
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(minutePlan));
        when(scooterMapper.updateById(scooter)).thenReturn(1);
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));

        Booking booking = bookingService.startScanRide("SC201");

        assertEquals(RentalConstants.RENTAL_TYPE_SCAN_RIDE, booking.getRentalType());
        assertEquals(RentalConstants.BOOKING_STATUS_IN_PROGRESS, booking.getStatus());
        assertEquals(SCOOTER_ID, booking.getScooterId());
        assertEquals(BigDecimal.ZERO, booking.getTotalCost());
        assertEquals(RentalConstants.SCOOTER_STATUS_IN_USE, scooter.getStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED, scooter.getLockStatus());
        assertEquals("MINUTE_1", booking.getHirePeriod());
        assertEquals("SC201", booking.getScooterCode());
    }

    @Test
    void listPickupScootersReturnsAvailableScootersForStore() {
        Booking booking = reservedBooking();
        Store store = enabledStore();
        Scooter scooterA = availableScooter();
        Scooter scooterB = Scooter.builder()
                .id(4L)
                .scooterCode("SC004")
                .storeId(STORE_ID)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_AVAILABLE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED)
                .build();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(scooterMapper.selectList(any())).thenReturn(List.of(scooterA, scooterB));
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));

        List<Scooter> scooters = bookingService.listPickupScooters(BOOKING_ID);

        assertEquals(2, scooters.size());
        assertEquals("Xipu North Hub", scooters.get(0).getStoreName());
        assertEquals(store.getAddress(), scooters.get(0).getLocation());
    }

    @Test
    void pickupBookingBindsSelectedScooterAndMarksBookingInProgress() {
        Booking booking = reservedBooking();
        PricingPlan dayPlan = pricingPlan(2L, "DAY_1", "30.00");
        Store store = enabledStore();
        Scooter scooter = availableScooter();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(scooterMapper.selectById(SCOOTER_ID)).thenReturn(scooter);
        when(bookingMapper.updateById(booking)).thenReturn(1);
        when(scooterMapper.updateById(scooter)).thenReturn(1);
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(dayPlan));

        Booking picked = bookingService.pickupBooking(BOOKING_ID, SCOOTER_ID);

        assertEquals(RentalConstants.BOOKING_STATUS_IN_PROGRESS, booking.getStatus());
        assertEquals(SCOOTER_ID, booking.getScooterId());
        assertNotNull(booking.getPickupTime());
        assertEquals(RentalConstants.SCOOTER_STATUS_IN_USE, scooter.getStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED, scooter.getLockStatus());
        assertEquals("SC003", picked.getScooterCode());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED, picked.getLockStatus());
    }

    @Test
    void pickupBookingRejectsBeforeAppointmentStart() {
        Booking booking = reservedBooking();
        booking.setStartTime(LocalDateTime.of(2026, 4, 15, 11, 0));
        booking.setPickupDeadline(LocalDateTime.of(2026, 4, 15, 11, 30));

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.pickupBooking(BOOKING_ID, SCOOTER_ID));

        assertEquals("Pickup is not open yet", error.getMessage());
        verify(scooterMapper, never()).selectById(any());
    }

    @Test
    void lockScooterUpdatesCurrentScooterLockState() {
        Booking booking = inProgressBooking();
        PricingPlan dayPlan = pricingPlan(2L, "DAY_1", "30.00");
        Store store = enabledStore();
        Scooter scooter = Scooter.builder()
                .id(SCOOTER_ID)
                .scooterCode("SC003")
                .storeId(STORE_ID)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_IN_USE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED)
                .build();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(scooterMapper.selectById(SCOOTER_ID)).thenReturn(scooter);
        when(scooterMapper.updateById(scooter)).thenReturn(1);
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(dayPlan));

        Booking locked = bookingService.lockScooter(BOOKING_ID);

        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED, scooter.getLockStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED, locked.getLockStatus());
    }

    @Test
    void returnBookingCalculatesRoundedOverdueCostAndMarksAwaitingPayment() {
        Booking booking = inProgressBooking();
        PricingPlan bookingPlan = pricingPlan(2L, "DAY_1", "30.00");
        PricingPlan hourlyPlan = pricingPlan(1L, "HOUR_1", "5.00");
        Store store = enabledStore();
        Scooter scooter = Scooter.builder()
                .id(SCOOTER_ID)
                .scooterCode("SC003")
                .storeId(STORE_ID)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_IN_USE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED)
                .build();

        booking.setEndTime(LocalDateTime.of(2026, 4, 15, 9, 50));

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(scooterMapper.selectById(SCOOTER_ID)).thenReturn(scooter);
        when(storeMapper.selectById(STORE_ID)).thenReturn(store);
        when(pricingPlanMapper.selectOne(any())).thenReturn(hourlyPlan);
        when(bookingMapper.updateById(booking)).thenReturn(1);
        when(scooterMapper.updateById(scooter)).thenReturn(1);
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(bookingPlan));

        BookingSettlementResult result = bookingService.returnBooking(BOOKING_ID);

        assertEquals(new BigDecimal("5.00"), booking.getOverdueCost());
        assertEquals(new BigDecimal("35.00"), booking.getTotalCost());
        assertNotNull(booking.getReturnTime());
        assertEquals(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT, result.getBooking().getStatus());
        assertEquals(RentalConstants.SCOOTER_STATUS_AVAILABLE, scooter.getStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED, scooter.getLockStatus());
        assertNull(result.getPayment());
    }

    @Test
    void returnScanRideCalculatesRoundedMinuteCostAndMarksAwaitingPayment() {
        Booking booking = scanRideBooking();
        PricingPlan minutePlan = pricingPlan(9L, "MINUTE_1", "1.00");
        Scooter scooter = scanRideScooterInUse();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(pricingPlanMapper.selectOne(any())).thenReturn(minutePlan);
        when(scooterMapper.selectById(SCOOTER_ID)).thenReturn(scooter);
        when(bookingMapper.updateById(booking)).thenReturn(1);
        when(scooterMapper.updateById(scooter)).thenReturn(1);
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));
        when(pricingPlanMapper.selectBatchIds(any())).thenReturn(List.of(minutePlan));
        when(geoAddressService.reverseGeocode(new BigDecimal("103.985000"), new BigDecimal("30.769000")))
                .thenReturn("Xipu Return Point");

        BookingSettlementResult result = bookingService.returnScanRide(
                BOOKING_ID,
                new BigDecimal("103.985000"),
                new BigDecimal("30.769000")
        );

        assertEquals(new BigDecimal("4.00"), booking.getTotalCost());
        assertEquals(BigDecimal.ZERO, booking.getOverdueCost());
        assertEquals("Xipu Return Point", booking.getReturnLocation());
        assertEquals(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT, result.getBooking().getStatus());
        assertEquals(RentalConstants.SCOOTER_STATUS_AVAILABLE, scooter.getStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED, scooter.getLockStatus());
        assertEquals(new BigDecimal("103.985000"), scooter.getLongitude());
        assertEquals(new BigDecimal("30.769000"), scooter.getLatitude());
        assertNull(result.getPayment());
    }

    @Test
    void expireReservationsMarksPastDeadlineBookingsAsNoShow() {
        Booking booking = reservedBooking();
        booking.setPickupDeadline(LocalDateTime.of(2026, 4, 15, 9, 0));

        when(bookingMapper.selectList(any())).thenReturn(List.of(Booking.builder().id(BOOKING_ID).build()));
        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        int expired = bookingService.expireReservations();

        assertEquals(1, expired);
        assertEquals(RentalConstants.BOOKING_STATUS_NO_SHOW_CANCELLED, booking.getStatus());
    }

    @Test
    void markOverdueBookingsUpdatesInProgressBookingsPastPlannedEnd() {
        Booking booking = inProgressBooking();
        booking.setEndTime(LocalDateTime.of(2026, 4, 15, 9, 0));

        when(bookingMapper.selectList(any())).thenReturn(List.of(Booking.builder().id(BOOKING_ID).build()));
        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(bookingMapper.updateById(booking)).thenReturn(1);

        int overdue = bookingService.markOverdueBookings();

        assertEquals(1, overdue);
        assertEquals(RentalConstants.BOOKING_STATUS_OVERDUE, booking.getStatus());
    }

    private Store enabledStore() {
        return Store.builder()
                .id(STORE_ID)
                .name("Xipu North Hub")
                .address("Xipu Campus Library North Plaza")
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .status(RentalConstants.STORE_STATUS_ENABLED)
                .build();
    }

    private PricingPlan pricingPlan(Long id, String hirePeriod, String price) {
        return PricingPlan.builder()
                .id(id)
                .hirePeriod(hirePeriod)
                .price(new BigDecimal(price))
                .build();
    }

    private Booking reservedBooking() {
        return Booking.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .storeId(STORE_ID)
                .pricingPlanId(2L)
                .rentalType(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .startTime(LocalDateTime.of(2026, 4, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 16, 9, 0))
                .pickupDeadline(LocalDateTime.of(2026, 4, 15, 10, 30))
                .totalCost(new BigDecimal("30.00"))
                .overdueCost(BigDecimal.ZERO)
                .status(RentalConstants.BOOKING_STATUS_RESERVED)
                .build();
    }

    private Booking inProgressBooking() {
        Booking booking = reservedBooking();
        booking.setScooterId(SCOOTER_ID);
        booking.setPickupTime(LocalDateTime.of(2026, 4, 15, 9, 10));
        booking.setStatus(RentalConstants.BOOKING_STATUS_IN_PROGRESS);
        return booking;
    }

    private Scooter availableScooter() {
        return Scooter.builder()
                .id(SCOOTER_ID)
                .scooterCode("SC003")
                .storeId(STORE_ID)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_AVAILABLE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED)
                .build();
    }

    private Booking scanRideBooking() {
        return Booking.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .scooterId(SCOOTER_ID)
                .pricingPlanId(9L)
                .rentalType(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .startTime(LocalDateTime.of(2026, 4, 15, 9, 56))
                .pickupTime(LocalDateTime.of(2026, 4, 15, 9, 56))
                .pickupLocation("Xipu East Roadside")
                .pickupLongitude(new BigDecimal("103.982120"))
                .pickupLatitude(new BigDecimal("30.767320"))
                .totalCost(BigDecimal.ZERO)
                .overdueCost(BigDecimal.ZERO)
                .status(RentalConstants.BOOKING_STATUS_IN_PROGRESS)
                .build();
    }

    private Scooter scanRideScooter() {
        return Scooter.builder()
                .id(SCOOTER_ID)
                .scooterCode("SC201")
                .rentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .status(RentalConstants.SCOOTER_STATUS_AVAILABLE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED)
                .location("Xipu East Roadside")
                .longitude(new BigDecimal("103.982120"))
                .latitude(new BigDecimal("30.767320"))
                .build();
    }

    private Scooter scanRideScooterInUse() {
        Scooter scooter = scanRideScooter();
        scooter.setStatus(RentalConstants.SCOOTER_STATUS_IN_USE);
        scooter.setLockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED);
        return scooter;
    }
}
