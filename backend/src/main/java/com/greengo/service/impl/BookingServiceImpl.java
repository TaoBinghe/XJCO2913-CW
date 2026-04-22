package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.Booking;
import com.greengo.domain.BookingSettlementResult;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.BookingService;
import com.greengo.service.DistributedLockService;
import com.greengo.service.GeoAddressService;
import com.greengo.utils.PricingPlanPeriodUtil;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.LockKeys;
import com.greengo.utils.RentalConstants;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private static final String OVERDUE_RATE_MISSING = "Pricing plan HOUR_1 is required for overdue settlement";

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired(required = false)
    private GeoAddressService geoAddressService;

    @Autowired(required = false)
    private DistributedLockService distributedLockService = new LocalDistributedLockService();

    @Autowired
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Cacheable(value = RedisCacheNames.PRICING_PLAN_LIST, key = "'reservation'")
    public List<PricingPlan> listPricingPlan() {
        return pricingPlanMapper.selectList(new QueryWrapper<PricingPlan>().orderByAsc("price")).stream()
                .filter(plan -> PricingPlanPeriodUtil.isReservationHirePeriod(plan.getHirePeriod()))
                .toList();
    }

    @Override
    @Transactional
    public Booking createStoreBooking(Long storeId, LocalDateTime appointmentStart, String hiredPeriod) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store is required");
        }
        Long userId = currentUserId();
        return distributedLockService.executeWithLocks(
                List.of(LockKeys.userBookingLock(userId), LockKeys.storeLock(storeId)),
                () -> doCreateStoreBooking(userId, storeId, appointmentStart, hiredPeriod)
        );
    }

    @Override
    @Transactional
    public Booking cancelStoreBooking(Long bookingId) {
        return distributedLockService.executeWithLock(
                LockKeys.bookingLock(bookingId),
                () -> doCancelStoreBooking(bookingId)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public Booking startScanRide(String scooterCode) {
        if (scooterCode == null || scooterCode.isBlank()) {
            throw new IllegalArgumentException("Scooter code is required");
        }
        Long userId = currentUserId();
        return distributedLockService.executeWithLock(LockKeys.userBookingLock(userId), () -> {
            Scooter scooter = findScooterByCode(scooterCode);
            if (scooter == null) {
                throw new IllegalArgumentException("Scooter not found");
            }
            return distributedLockService.executeWithLock(
                    LockKeys.scooterLock(scooter.getId()),
                    () -> doStartScanRide(userId, scooter.getId(), scooterCode.trim())
            );
        });
    }

    @Override
    public List<Scooter> listPickupScooters(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        requireRentalType(booking, RentalConstants.RENTAL_TYPE_STORE_PICKUP, "Only store pickup bookings can list pickup scooters");
        if (!RentalConstants.BOOKING_STATUS_RESERVED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only reserved bookings can list pickup scooters");
        }
        List<Scooter> scooters = scooterMapper.selectList(
                new LambdaQueryWrapper<Scooter>()
                        .eq(Scooter::getStoreId, booking.getStoreId())
                        .eq(Scooter::getRentalMode, RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                        .eq(Scooter::getStatus, RentalConstants.SCOOTER_STATUS_AVAILABLE)
                        .orderByAsc(Scooter::getScooterCode)
        );
        enrichScooters(scooters);
        return scooters;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public Booking pickupBooking(Long bookingId, Long scooterId) {
        if (scooterId == null) {
            throw new IllegalArgumentException("Pickup scooter is required");
        }
        return distributedLockService.executeWithLocks(
                List.of(LockKeys.bookingLock(bookingId), LockKeys.scooterLock(scooterId)),
                () -> doPickupBooking(bookingId, scooterId)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public Booking lockScooter(Long bookingId) {
        return updateScooterLockStatus(bookingId, RentalConstants.SCOOTER_LOCK_STATUS_LOCKED);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public Booking unlockScooter(Long bookingId) {
        return updateScooterLockStatus(bookingId, RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public BookingSettlementResult returnBooking(Long bookingId) {
        return distributedLockService.executeWithLock(LockKeys.bookingLock(bookingId), () -> {
            Booking booking = getOwnedBooking(bookingId);
            requireRentalType(booking, RentalConstants.RENTAL_TYPE_STORE_PICKUP, "This booking must be returned through the store pickup flow");
            if (booking.getScooterId() == null) {
                throw new IllegalArgumentException("Booking has no picked scooter");
            }
            return distributedLockService.executeWithLock(
                    LockKeys.scooterLock(booking.getScooterId()),
                    () -> doReturnBooking(booking, null, null)
            );
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public BookingSettlementResult returnScanRide(Long bookingId, BigDecimal longitude, BigDecimal latitude) {
        return distributedLockService.executeWithLock(LockKeys.bookingLock(bookingId), () -> {
            Booking booking = getOwnedBooking(bookingId);
            requireRentalType(booking, RentalConstants.RENTAL_TYPE_SCAN_RIDE, "Only scan ride bookings can use scan return");
            if (booking.getScooterId() == null) {
                throw new IllegalArgumentException("Booking has no scooter");
            }
            return distributedLockService.executeWithLock(
                    LockKeys.scooterLock(booking.getScooterId()),
                    () -> doReturnBooking(booking, longitude, latitude)
            );
        });
    }

    @Override
    @Transactional
    public int expireReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> bookingIds = baseMapper.selectList(
                        new QueryWrapper<Booking>()
                                .select("id")
                                .eq("rental_type", RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                                .eq("status", RentalConstants.BOOKING_STATUS_RESERVED)
                                .lt("pickup_deadline", now)
                ).stream()
                .map(Booking::getId)
                .toList();

        int updated = 0;
        for (Long bookingId : bookingIds) {
            Boolean changed = distributedLockService.executeWithLock(
                    LockKeys.bookingLock(bookingId),
                    () -> expireReservationIfNeeded(bookingId, now)
            );
            if (Boolean.TRUE.equals(changed)) {
                updated++;
            }
        }
        return updated;
    }

    @Override
    @Transactional
    public int markOverdueBookings() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> bookingIds = baseMapper.selectList(
                        new QueryWrapper<Booking>()
                                .select("id")
                                .eq("rental_type", RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                                .eq("status", RentalConstants.BOOKING_STATUS_IN_PROGRESS)
                                .lt("end_time", now)
                                .isNull("return_time")
                ).stream()
                .map(Booking::getId)
                .toList();

        int updated = 0;
        for (Long bookingId : bookingIds) {
            Boolean changed = distributedLockService.executeWithLock(
                    LockKeys.bookingLock(bookingId),
                    () -> markBookingOverdueIfNeeded(bookingId, now)
            );
            if (Boolean.TRUE.equals(changed)) {
                updated++;
            }
        }
        return updated;
    }

    @Override
    public boolean bookScooter(Integer scooterId, String hiredPeriod) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public boolean updateBookingStatus(Long bookingId, String status) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public boolean activateBooking(Long bookingId) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public Booking modifyBookingPeriod(Long bookingId, String hiredPeriod) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public Booking cancelBooking(Long bookingId) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public Booking renewBooking(Long bookingId, String hiredPeriod) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public Map<String, Object> finishBooking(Long bookingId) {
        throw new IllegalArgumentException(RentalConstants.LEGACY_BOOKING_DISABLED_MESSAGE);
    }

    @Override
    public List<Booking> listBookingsByUserId(Long userId) {
        List<Booking> bookings = baseMapper.selectList(
                new QueryWrapper<Booking>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at")
        );
        enrichBookings(bookings);
        return bookings;
    }

    private Booking doCreateStoreBooking(Long userId, Long storeId, LocalDateTime appointmentStart, String hiredPeriod) {
        validateAppointmentWindow(appointmentStart);
        PricingPlan pricingPlan = findStorePricingPlanByHirePeriod(hiredPeriod);
        LocalDateTime appointmentEnd = PricingPlanPeriodUtil.addPeriod(appointmentStart, pricingPlan.getHirePeriod());

        ensureNoOpenBooking(userId, null, "You already have an unfinished booking");
        Store store = storeMapper.selectById(storeId);
        if (store == null || !RentalConstants.STORE_STATUS_ENABLED.equals(store.getStatus())) {
            throw new IllegalArgumentException("Store is not available");
        }
        ensureStoreHasBookableInventory(storeId, appointmentStart, appointmentEnd, null);

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setStoreId(storeId);
        booking.setPricingPlanId(pricingPlan.getId());
        booking.setRentalType(RentalConstants.RENTAL_TYPE_STORE_PICKUP);
        booking.setStartTime(appointmentStart);
        booking.setEndTime(appointmentEnd);
        booking.setPickupDeadline(appointmentStart.plusMinutes(30));
        booking.setTotalCost(requirePrice(pricingPlan));
        booking.setOverdueCost(BigDecimal.ZERO);
        booking.setStatus(RentalConstants.BOOKING_STATUS_RESERVED);

        if (baseMapper.insert(booking) <= 0) {
            throw new IllegalArgumentException("Failed to create booking");
        }
        return getOwnedBookingDetail(booking.getId());
    }

    private Booking doStartScanRide(Long userId, Long scooterId, String scooterCode) {
        ensureNoOpenBooking(userId, null, "You already have an unfinished booking");

        Scooter scooter = findScooterByCode(scooterCode);
        if (scooter == null || !Objects.equals(scooter.getId(), scooterId)) {
            throw new IllegalArgumentException("Scooter not found");
        }
        if (!RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(scooter.getRentalMode())) {
            throw new IllegalArgumentException("Scooter is not configured for scan ride");
        }
        if (!RentalConstants.SCOOTER_STATUS_AVAILABLE.equals(scooter.getStatus())) {
            throw new IllegalArgumentException("Scooter is not available");
        }

        PricingPlan minutePlan = findRequiredPricingPlan("MINUTE_1");
        if (minutePlan == null || minutePlan.getPrice() == null) {
            throw new IllegalArgumentException("Pricing plan MINUTE_1 is required for scan ride");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setScooterId(scooter.getId());
        booking.setPricingPlanId(minutePlan.getId());
        booking.setStoreId(null);
        booking.setRentalType(RentalConstants.RENTAL_TYPE_SCAN_RIDE);
        booking.setStartTime(now);
        booking.setPickupTime(now);
        booking.setEndTime(null);
        booking.setPickupDeadline(null);
        booking.setPickupLocation(scooter.getLocation());
        booking.setPickupLongitude(scooter.getLongitude());
        booking.setPickupLatitude(scooter.getLatitude());
        booking.setTotalCost(BigDecimal.ZERO);
        booking.setOverdueCost(BigDecimal.ZERO);
        booking.setStatus(RentalConstants.BOOKING_STATUS_IN_PROGRESS);
        if (baseMapper.insert(booking) <= 0) {
            throw new IllegalArgumentException("Failed to start scan ride");
        }

        scooter.setStatus(RentalConstants.SCOOTER_STATUS_IN_USE);
        scooter.setLockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED);
        if (scooterMapper.updateById(scooter) <= 0) {
            throw new IllegalArgumentException("Failed to update scooter for scan ride");
        }
        return getOwnedBookingDetail(booking.getId());
    }

    private Booking doCancelStoreBooking(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        requireRentalType(booking, RentalConstants.RENTAL_TYPE_STORE_PICKUP, "Only store pickup bookings can be cancelled");
        if (!RentalConstants.BOOKING_STATUS_RESERVED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only reserved bookings can be cancelled");
        }
        booking.setStatus(RentalConstants.BOOKING_STATUS_CANCELLED);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to cancel booking");
        }
        return getOwnedBookingDetail(bookingId);
    }

    private Booking doPickupBooking(Long bookingId, Long scooterId) {
        Booking booking = getOwnedBooking(bookingId);
        requireRentalType(booking, RentalConstants.RENTAL_TYPE_STORE_PICKUP, "Only store pickup bookings can be picked up");
        if (!RentalConstants.BOOKING_STATUS_RESERVED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only reserved bookings can be picked up");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.getStartTime() == null || now.isBefore(booking.getStartTime())) {
            throw new IllegalArgumentException("Pickup is not open yet");
        }
        if (booking.getPickupDeadline() == null || now.isAfter(booking.getPickupDeadline())) {
            throw new IllegalArgumentException("Pickup window has expired");
        }

        Scooter scooter = scooterMapper.selectById(scooterId);
        if (scooter == null
                || !Objects.equals(scooter.getStoreId(), booking.getStoreId())
                || !RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(scooter.getRentalMode())) {
            throw new IllegalArgumentException("Scooter does not belong to the booking store");
        }
        if (!RentalConstants.SCOOTER_STATUS_AVAILABLE.equals(scooter.getStatus())) {
            throw new IllegalArgumentException("Scooter is not available for pickup");
        }

        booking.setScooterId(scooterId);
        booking.setPickupTime(now);
        booking.setPickupLocation(scooter.getLocation());
        booking.setPickupLongitude(scooter.getLongitude());
        booking.setPickupLatitude(scooter.getLatitude());
        booking.setStatus(RentalConstants.BOOKING_STATUS_IN_PROGRESS);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to pick up booking");
        }

        scooter.setStatus(RentalConstants.SCOOTER_STATUS_IN_USE);
        scooter.setLockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED);
        if (scooterMapper.updateById(scooter) <= 0) {
            throw new IllegalArgumentException("Failed to update scooter for pickup");
        }
        return getOwnedBookingDetail(bookingId);
    }

    private Booking updateScooterLockStatus(Long bookingId, String lockStatus) {
        return distributedLockService.executeWithLock(LockKeys.bookingLock(bookingId), () -> {
            Booking booking = getOwnedBooking(bookingId);
            if (booking.getScooterId() == null) {
                throw new IllegalArgumentException("Booking has no picked scooter");
            }
            if (!RentalConstants.BOOKING_STATUS_IN_PROGRESS.equals(booking.getStatus())
                    && !RentalConstants.BOOKING_STATUS_OVERDUE.equals(booking.getStatus())) {
                throw new IllegalArgumentException("Only active or overdue bookings can change scooter lock status");
            }
            return distributedLockService.executeWithLock(LockKeys.scooterLock(booking.getScooterId()), () -> {
                Scooter scooter = scooterMapper.selectById(booking.getScooterId());
                if (scooter == null) {
                    throw new IllegalArgumentException("Scooter not found");
                }
                scooter.setLockStatus(lockStatus);
                if (scooterMapper.updateById(scooter) <= 0) {
                    throw new IllegalArgumentException("Failed to update scooter lock status");
                }
                return getOwnedBookingDetail(bookingId);
            });
        });
    }

    private BookingSettlementResult doReturnBooking(Booking booking, BigDecimal longitude, BigDecimal latitude) {
        if (!RentalConstants.BOOKING_STATUS_IN_PROGRESS.equals(booking.getStatus())
                && !RentalConstants.BOOKING_STATUS_OVERDUE.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only active or overdue bookings can be returned");
        }

        Scooter scooter = scooterMapper.selectById(booking.getScooterId());
        if (scooter == null) {
            throw new IllegalArgumentException("Scooter not found");
        }

        LocalDateTime returnTime = LocalDateTime.now(clock);
        BigDecimal totalCost;
        BigDecimal overdueCost = BigDecimal.ZERO;

        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())) {
            Store store = storeMapper.selectById(booking.getStoreId());
            if (store == null) {
                throw new IllegalArgumentException("Store not found");
            }

            overdueCost = calculateOverdueCost(booking, returnTime);
            BigDecimal baseCost = resolveBaseCost(booking);
            totalCost = baseCost.add(overdueCost);

            booking.setReturnLocation(store.getAddress());
            booking.setReturnLongitude(store.getLongitude());
            booking.setReturnLatitude(store.getLatitude());
        } else if (RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(booking.getRentalType())) {
            validateRideCoordinates(longitude, latitude);
            totalCost = calculateScanRideCost(booking, returnTime);
            booking.setEndTime(returnTime);
            booking.setReturnLocation(resolveLocationText(longitude, latitude));
            booking.setReturnLongitude(longitude);
            booking.setReturnLatitude(latitude);
        } else {
            throw new IllegalArgumentException("Unsupported rental type");
        }

        booking.setReturnTime(returnTime);
        booking.setOverdueCost(overdueCost);
        booking.setTotalCost(totalCost);
        booking.setStatus(RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT);
        if (baseMapper.updateById(booking) <= 0) {
            throw new IllegalArgumentException("Failed to update booking after return");
        }

        scooter.setStatus(RentalConstants.SCOOTER_STATUS_AVAILABLE);
        scooter.setLockStatus(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED);
        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())) {
            Store store = storeMapper.selectById(booking.getStoreId());
            scooter.setStoreId(store.getId());
            scooter.setLocation(store.getAddress());
            scooter.setLongitude(store.getLongitude());
            scooter.setLatitude(store.getLatitude());
        } else {
            scooter.setStoreId(null);
            scooter.setLocation(booking.getReturnLocation());
            scooter.setLongitude(booking.getReturnLongitude());
            scooter.setLatitude(booking.getReturnLatitude());
        }
        if (scooterMapper.updateById(scooter) <= 0) {
            throw new IllegalArgumentException("Failed to update scooter after return");
        }

        return new BookingSettlementResult(getOwnedBookingDetail(booking.getId()), null);
    }

    private Boolean expireReservationIfNeeded(Long bookingId, LocalDateTime now) {
        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null
                || !RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())
                || !RentalConstants.BOOKING_STATUS_RESERVED.equals(booking.getStatus())) {
            return false;
        }
        if (booking.getPickupDeadline() == null || !now.isAfter(booking.getPickupDeadline())) {
            return false;
        }
        booking.setStatus(RentalConstants.BOOKING_STATUS_NO_SHOW_CANCELLED);
        return baseMapper.updateById(booking) > 0;
    }

    private Boolean markBookingOverdueIfNeeded(Long bookingId, LocalDateTime now) {
        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null
                || !RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())
                || !RentalConstants.BOOKING_STATUS_IN_PROGRESS.equals(booking.getStatus())) {
            return false;
        }
        if (booking.getReturnTime() != null || booking.getEndTime() == null || !now.isAfter(booking.getEndTime())) {
            return false;
        }
        booking.setStatus(RentalConstants.BOOKING_STATUS_OVERDUE);
        return baseMapper.updateById(booking) > 0;
    }

    private Booking getOwnedBooking(Long bookingId) {
        Long userId = currentUserId();
        Booking booking = baseMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("Not your booking");
        }
        return booking;
    }

    private Booking getOwnedBookingDetail(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        enrichBookings(List.of(booking));
        return booking;
    }

    private Long currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return ((Number) claims.get("id")).longValue();
    }

    private void validateAppointmentWindow(LocalDateTime appointmentStart) {
        if (appointmentStart == null) {
            throw new IllegalArgumentException("Appointment start is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (appointmentStart.isBefore(now)) {
            throw new IllegalArgumentException("Appointment start must be in the future");
        }
        if (appointmentStart.isAfter(now.plusDays(30))) {
            throw new IllegalArgumentException("Appointment start cannot be more than 30 days ahead");
        }
    }

    private PricingPlan findStorePricingPlanByHirePeriod(String hiredPeriod) {
        String normalizedHirePeriod = PricingPlanPeriodUtil.normalizeHirePeriod(hiredPeriod);
        if (normalizedHirePeriod == null || !PricingPlanPeriodUtil.isReservationHirePeriod(normalizedHirePeriod)) {
            throw new IllegalArgumentException("Pricing plan not found");
        }
        PricingPlan pricingPlan = findRequiredPricingPlan(normalizedHirePeriod);
        if (pricingPlan == null) {
            throw new IllegalArgumentException("Pricing plan not found");
        }
        return pricingPlan;
    }

    private PricingPlan findRequiredPricingPlan(String hirePeriod) {
        return pricingPlanMapper.selectOne(new QueryWrapper<PricingPlan>().eq("hire_period", hirePeriod));
    }

    private void ensureNoOpenBooking(Long userId, Long excludeBookingId, String message) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .in("status",
                        RentalConstants.BOOKING_STATUS_RESERVED,
                        RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                        RentalConstants.BOOKING_STATUS_OVERDUE);
        if (excludeBookingId != null) {
            wrapper.ne("id", excludeBookingId);
        }
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureStoreHasBookableInventory(Long storeId,
                                                 LocalDateTime appointmentStart,
                                                 LocalDateTime appointmentEnd,
                                                 Long excludeBookingId) {
        int fleetSize = countRentableStoreScooters(storeId);
        if (fleetSize <= 0) {
            throw new IllegalArgumentException("Store has no available inventory");
        }
        int overlapCount = countOverlappingStoreBookings(storeId, appointmentStart, appointmentEnd, excludeBookingId);
        if (overlapCount >= fleetSize) {
            throw new IllegalArgumentException("Store inventory is fully booked for the selected time");
        }
    }

    private int countRentableStoreScooters(Long storeId) {
        Long count = scooterMapper.selectCount(
                new QueryWrapper<Scooter>()
                        .eq("store_id", storeId)
                        .eq("rental_mode", RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                        .notIn("status",
                                RentalConstants.SCOOTER_STATUS_DISABLED,
                                RentalConstants.SCOOTER_STATUS_MAINTENANCE)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countOverlappingStoreBookings(Long storeId,
                                              LocalDateTime appointmentStart,
                                              LocalDateTime appointmentEnd,
                                              Long excludeBookingId) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq("rental_type", RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .in("status",
                        RentalConstants.BOOKING_STATUS_RESERVED,
                        RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                        RentalConstants.BOOKING_STATUS_OVERDUE)
                .lt("start_time", appointmentEnd)
                .gt("end_time", appointmentStart);
        if (excludeBookingId != null) {
            wrapper.ne("id", excludeBookingId);
        }
        Long count = baseMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private BigDecimal requirePrice(PricingPlan pricingPlan) {
        if (pricingPlan.getPrice() == null) {
            throw new IllegalArgumentException("Pricing plan price is missing");
        }
        return pricingPlan.getPrice();
    }

    private BigDecimal calculateOverdueCost(Booking booking, LocalDateTime returnTime) {
        if (booking.getEndTime() == null || returnTime == null || !returnTime.isAfter(booking.getEndTime())) {
            return BigDecimal.ZERO;
        }
        PricingPlan hourlyPlan = findRequiredPricingPlan("HOUR_1");
        if (hourlyPlan == null || hourlyPlan.getPrice() == null) {
            throw new IllegalArgumentException(OVERDUE_RATE_MISSING);
        }
        long seconds = Duration.between(booking.getEndTime(), returnTime).getSeconds();
        long overdueHours = (seconds + 3599L) / 3600L;
        return hourlyPlan.getPrice().multiply(BigDecimal.valueOf(overdueHours));
    }

    private BigDecimal resolveBaseCost(Booking booking) {
        BigDecimal totalCost = booking.getTotalCost() == null ? BigDecimal.ZERO : booking.getTotalCost();
        BigDecimal overdueCost = booking.getOverdueCost() == null ? BigDecimal.ZERO : booking.getOverdueCost();
        BigDecimal baseCost = totalCost.subtract(overdueCost);
        return baseCost.compareTo(BigDecimal.ZERO) >= 0 ? baseCost : totalCost;
    }

    private BigDecimal calculateScanRideCost(Booking booking, LocalDateTime returnTime) {
        PricingPlan minutePlan = findRequiredPricingPlan("MINUTE_1");
        if (minutePlan == null || minutePlan.getPrice() == null) {
            throw new IllegalArgumentException("Pricing plan MINUTE_1 is required for scan ride settlement");
        }
        LocalDateTime rideStart = booking.getPickupTime() != null ? booking.getPickupTime() : booking.getStartTime();
        if (rideStart == null || returnTime == null) {
            return BigDecimal.ZERO;
        }
        long seconds = Duration.between(rideStart, returnTime).getSeconds();
        long chargedMinutes = seconds <= 0 ? 0 : (seconds + 59L) / 60L;
        return minutePlan.getPrice().multiply(BigDecimal.valueOf(chargedMinutes));
    }

    private Scooter findScooterByCode(String scooterCode) {
        if (scooterCode == null || scooterCode.isBlank()) {
            return null;
        }
        return scooterMapper.selectOne(new LambdaQueryWrapper<Scooter>()
                .eq(Scooter::getScooterCode, scooterCode.trim()));
    }

    private void requireRentalType(Booking booking, String expectedRentalType, String message) {
        if (!Objects.equals(expectedRentalType, booking.getRentalType())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateRideCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("Return coordinates are required");
        }
        if (!isBetween(longitude, new BigDecimal("-180"), new BigDecimal("180"))
                || !isBetween(latitude, new BigDecimal("-90"), new BigDecimal("90"))) {
            throw new IllegalArgumentException("Return coordinates are invalid");
        }
    }

    private boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private String resolveLocationText(BigDecimal longitude, BigDecimal latitude) {
        if (geoAddressService != null) {
            String location = geoAddressService.reverseGeocode(longitude, latitude);
            if (location != null && !location.isBlank()) {
                return location;
            }
        }
        return longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
    }

    private void enrichBookings(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return;
        }

        Map<Long, Store> storeMap = loadStores(bookings);
        Map<Long, Scooter> scooterMap = loadScooters(bookings);
        Map<Long, PricingPlan> pricingPlanMap = loadPricingPlans(bookings);

        for (Booking booking : bookings) {
            Store store = booking.getStoreId() == null ? null : storeMap.get(booking.getStoreId());
            if (store != null) {
                booking.setStoreName(store.getName());
                booking.setStoreAddress(store.getAddress());
                booking.setStoreLongitude(store.getLongitude());
                booking.setStoreLatitude(store.getLatitude());
            }

            Scooter scooter = booking.getScooterId() == null ? null : scooterMap.get(booking.getScooterId());
            if (scooter != null) {
                booking.setScooterCode(scooter.getScooterCode());
                booking.setScooterStatus(scooter.getStatus());
                booking.setLockStatus(scooter.getLockStatus());
            }

            PricingPlan pricingPlan = booking.getPricingPlanId() == null ? null : pricingPlanMap.get(booking.getPricingPlanId());
            if (pricingPlan != null) {
                booking.setHirePeriod(pricingPlan.getHirePeriod());
            }
        }
    }

    private void enrichScooters(List<Scooter> scooters) {
        if (scooters == null || scooters.isEmpty()) {
            return;
        }
        List<Long> storeIds = scooters.stream()
                .map(Scooter::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Store> storeMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            storeMapper.selectBatchIds(storeIds).forEach(store -> storeMap.put(store.getId(), store));
        }
        for (Scooter scooter : scooters) {
            Store store = storeMap.get(scooter.getStoreId());
            if (store == null) {
                continue;
            }
            scooter.setStoreName(store.getName());
            scooter.setStoreAddress(store.getAddress());
            scooter.setLocation(store.getAddress());
            scooter.setLongitude(store.getLongitude());
            scooter.setLatitude(store.getLatitude());
        }
    }

    private Map<Long, Store> loadStores(List<Booking> bookings) {
        List<Long> storeIds = bookings.stream()
                .map(Booking::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectBatchIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, store -> store));
    }

    private Map<Long, Scooter> loadScooters(List<Booking> bookings) {
        List<Long> scooterIds = bookings.stream()
                .map(Booking::getScooterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (scooterIds.isEmpty()) {
            return Map.of();
        }
        return scooterMapper.selectBatchIds(scooterIds).stream()
                .collect(Collectors.toMap(Scooter::getId, scooter -> scooter));
    }

    private Map<Long, PricingPlan> loadPricingPlans(List<Booking> bookings) {
        List<Long> pricingPlanIds = bookings.stream()
                .map(Booking::getPricingPlanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (pricingPlanIds.isEmpty()) {
            return Map.of();
        }
        return pricingPlanMapper.selectBatchIds(pricingPlanIds).stream()
                .collect(Collectors.toMap(PricingPlan::getId, pricingPlan -> pricingPlan));
    }
}
