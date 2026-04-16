package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.greengo.domain.Booking;
import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.GeoAddressService;
import com.greengo.service.StoreService;
import com.greengo.utils.PricingPlanPeriodUtil;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.RentalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private GeoAddressService geoAddressService;

    @Autowired
    private Clock clock = Clock.systemDefaultZone();

    @Override
    public List<Store> listEnabledStores(LocalDateTime appointmentStart, String hiredPeriod) {
        validateAvailabilityWindow(appointmentStart, hiredPeriod);
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, RentalConstants.STORE_STATUS_ENABLED)
                .orderByAsc(Store::getId);
        List<Store> stores = storeMapper.selectList(wrapper);
        stores.forEach(store -> enrichInventory(store, appointmentStart, hiredPeriod));
        return stores;
    }

    @Override
    public Store getEnabledStore(Long storeId, LocalDateTime appointmentStart, String hiredPeriod) {
        validateAvailabilityWindow(appointmentStart, hiredPeriod);
        Store store = storeMapper.selectById(storeId);
        if (store == null || !RentalConstants.STORE_STATUS_ENABLED.equals(store.getStatus())) {
            return null;
        }
        enrichInventory(store, appointmentStart, hiredPeriod);
        return store;
    }

    @Override
    public List<Store> listAllStores() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().orderByAsc(Store::getId));
        stores.forEach(store -> enrichInventory(store, null, null));
        return stores;
    }

    @Override
    public Store getStoreById(Long id) {
        Store store = storeMapper.selectById(id);
        if (store != null) {
            enrichInventory(store, null, null);
        }
        return store;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean createStore(Store store) {
        if (!isValidStore(store, false)) {
            return false;
        }
        hydrateAddress(store);
        return storeMapper.insert(store) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean updateStore(Long id, Store store) {
        if (id == null || store == null) {
            return false;
        }
        Store existing = storeMapper.selectById(id);
        if (existing == null) {
            return false;
        }

        if (store.getName() != null && !store.getName().isBlank()) {
            existing.setName(store.getName().trim());
        }
        if (store.getLongitude() != null) {
            existing.setLongitude(store.getLongitude());
        }
        if (store.getLatitude() != null) {
            existing.setLatitude(store.getLatitude());
        }
        if (store.getStatus() != null) {
            existing.setStatus(normalizeStoreStatus(store.getStatus()));
        }
        if (store.getAddress() != null && !store.getAddress().isBlank()) {
            existing.setAddress(store.getAddress().trim());
        }
        if (!isValidStore(existing, true)) {
            return false;
        }
        hydrateAddress(existing);
        if (storeMapper.updateById(existing) <= 0) {
            return false;
        }
        syncScootersWithStore(existing);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean deleteStore(Long id) {
        if (id == null || storeMapper.selectById(id) == null) {
            return false;
        }
        Long scooterCount = scooterMapper.selectCount(new LambdaQueryWrapper<Scooter>().eq(Scooter::getStoreId, id));
        if (scooterCount != null && scooterCount > 0) {
            return false;
        }

        Long openBookingCount = bookingMapper.selectCount(
                new QueryWrapper<Booking>()
                        .eq("store_id", id)
                        .in("status",
                                RentalConstants.BOOKING_STATUS_RESERVED,
                                RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                                RentalConstants.BOOKING_STATUS_OVERDUE)
        );
        if (openBookingCount != null && openBookingCount > 0) {
            return false;
        }
        return storeMapper.deleteById(id) > 0;
    }

    private void enrichInventory(Store store, LocalDateTime appointmentStart, String hiredPeriod) {
        store.setAppointmentStart(appointmentStart);
        store.setAppointmentEnd(resolveAppointmentEnd(appointmentStart, hiredPeriod));
        store.setTotalInventory(countRentableScooters(store.getId()));
        store.setCurrentAvailableInventory(countCurrentlyAvailableScooters(store.getId()));
        if (appointmentStart != null && hiredPeriod != null && !hiredPeriod.isBlank()) {
            store.setBookableInventory(Math.max(store.getTotalInventory() - countOverlappingBookings(
                    store.getId(),
                    appointmentStart,
                    store.getAppointmentEnd(),
                    null
            ), 0));
        } else {
            store.setBookableInventory(store.getCurrentAvailableInventory());
        }
    }

    private int countRentableScooters(Long storeId) {
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

    private int countCurrentlyAvailableScooters(Long storeId) {
        Long count = scooterMapper.selectCount(
                new QueryWrapper<Scooter>()
                        .eq("store_id", storeId)
                        .eq("rental_mode", RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                        .eq("status", RentalConstants.SCOOTER_STATUS_AVAILABLE)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countOverlappingBookings(Long storeId,
                                         LocalDateTime appointmentStart,
                                         LocalDateTime appointmentEnd,
                                         Long excludeBookingId) {
        if (storeId == null || appointmentStart == null || appointmentEnd == null) {
            return 0;
        }
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .in("status",
                        RentalConstants.BOOKING_STATUS_RESERVED,
                        RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                        RentalConstants.BOOKING_STATUS_OVERDUE)
                .lt("start_time", appointmentEnd)
                .gt("end_time", appointmentStart);
        if (excludeBookingId != null) {
            wrapper.ne("id", excludeBookingId);
        }
        Long count = bookingMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private void validateAvailabilityWindow(LocalDateTime appointmentStart, String hiredPeriod) {
        boolean hasStart = appointmentStart != null;
        boolean hasPeriod = hiredPeriod != null && !hiredPeriod.isBlank();
        if (hasStart != hasPeriod) {
            throw new IllegalArgumentException("appointmentStart and hiredPeriod must be provided together");
        }
        if (hasStart) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (appointmentStart.isBefore(now)) {
                throw new IllegalArgumentException("Appointment start must be in the future");
            }
            if (appointmentStart.isAfter(now.plusDays(30))) {
                throw new IllegalArgumentException("Appointment start cannot be more than 30 days ahead");
            }
            if (!PricingPlanPeriodUtil.isReservationHirePeriod(hiredPeriod)) {
                throw new IllegalArgumentException(PricingPlanPeriodUtil.reservationFormatHint());
            }
        }
    }

    private LocalDateTime resolveAppointmentEnd(LocalDateTime appointmentStart, String hiredPeriod) {
        if (appointmentStart == null || hiredPeriod == null || hiredPeriod.isBlank()) {
            return null;
        }
        return PricingPlanPeriodUtil.addPeriod(appointmentStart, hiredPeriod);
    }

    private boolean isValidStore(Store store, boolean allowExistingAddress) {
        if (store == null || store.getName() == null || store.getName().isBlank()) {
            return false;
        }
        if (!hasValidCoordinates(store.getLongitude(), store.getLatitude())) {
            return false;
        }
        String normalizedStatus = normalizeStoreStatus(store.getStatus());
        if (normalizedStatus == null) {
            return false;
        }
        store.setStatus(normalizedStatus);
        if (allowExistingAddress && store.getAddress() != null && !store.getAddress().isBlank()) {
            store.setAddress(store.getAddress().trim());
        }
        return true;
    }

    private String normalizeStoreStatus(String status) {
        if (status == null || status.isBlank()) {
            return RentalConstants.STORE_STATUS_ENABLED;
        }
        if (RentalConstants.STORE_STATUS_ENABLED.equalsIgnoreCase(status)) {
            return RentalConstants.STORE_STATUS_ENABLED;
        }
        if (RentalConstants.STORE_STATUS_DISABLED.equalsIgnoreCase(status)) {
            return RentalConstants.STORE_STATUS_DISABLED;
        }
        return null;
    }

    private void hydrateAddress(Store store) {
        if (store.getAddress() != null && !store.getAddress().isBlank()) {
            store.setAddress(store.getAddress().trim());
            return;
        }
        String resolvedAddress = geoAddressService.reverseGeocode(store.getLongitude(), store.getLatitude());
        if (resolvedAddress == null || resolvedAddress.isBlank()) {
            throw new IllegalArgumentException("Failed to resolve store address from coordinates");
        }
        store.setAddress(resolvedAddress);
    }

    private boolean hasValidCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        return isBetween(longitude, new BigDecimal("-180"), new BigDecimal("180"))
                && isBetween(latitude, new BigDecimal("-90"), new BigDecimal("90"));
    }

    private boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private void syncScootersWithStore(Store store) {
        LambdaUpdateWrapper<Scooter> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Scooter::getStoreId, store.getId())
                .eq(Scooter::getRentalMode, RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .set(Scooter::getLocation, store.getAddress())
                .set(Scooter::getLongitude, store.getLongitude())
                .set(Scooter::getLatitude, store.getLatitude());
        scooterMapper.update(null, wrapper);
    }
}
