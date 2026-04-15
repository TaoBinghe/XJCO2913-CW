package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.Booking;
import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.ScooterService;
import com.greengo.utils.RedisCacheNames;
import com.greengo.utils.RentalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ScooterServiceImpl extends ServiceImpl<ScooterMapper, Scooter> implements ScooterService {

    private static final Set<String> VALID_SCOOTER_STATUSES = Set.of(
            RentalConstants.SCOOTER_STATUS_AVAILABLE,
            RentalConstants.SCOOTER_STATUS_IN_USE,
            RentalConstants.SCOOTER_STATUS_MAINTENANCE,
            RentalConstants.SCOOTER_STATUS_DISABLED
    );

    private static final Set<String> VALID_LOCK_STATUSES = Set.of(
            RentalConstants.SCOOTER_LOCK_STATUS_LOCKED,
            RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED
    );

    private static final Set<String> VALID_RENTAL_MODES = Set.of(
            RentalConstants.RENTAL_TYPE_STORE_PICKUP,
            RentalConstants.RENTAL_TYPE_SCAN_RIDE
    );

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Override
    @Cacheable(value = RedisCacheNames.SCOOTER_LIST, key = "'admin'")
    public List<Scooter> listAll() {
        List<Scooter> scooters = baseMapper.selectList(new LambdaQueryWrapper<Scooter>().orderByAsc(Scooter::getId));
        enrichScooters(scooters);
        return scooters;
    }

    @Override
    @Cacheable(value = RedisCacheNames.SCOOTER_LIST, key = "'map'")
    public List<Scooter> listMapScooters() {
        LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scooter::getRentalMode, RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .eq(Scooter::getStatus, RentalConstants.SCOOTER_STATUS_AVAILABLE)
                .orderByAsc(Scooter::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Scooter getById(Long id) {
        Scooter scooter = baseMapper.selectById(id);
        if (scooter != null) {
            enrichScooters(List.of(scooter));
        }
        return scooter;
    }

    @Override
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean addScooter(Scooter scooter) {
        if (scooter == null || scooter.getScooterCode() == null || scooter.getScooterCode().isBlank()) {
            return false;
        }
        if (!isUniqueScooterCode(scooter.getScooterCode().trim(), null)) {
            return false;
        }
        String rentalMode = normalizeRentalMode(scooter.getRentalMode());
        scooter.setScooterCode(scooter.getScooterCode().trim());
        scooter.setRentalMode(rentalMode);
        scooter.setStatus(normalizeScooterStatus(scooter.getStatus()));
        scooter.setLockStatus(normalizeLockStatus(scooter.getLockStatus()));
        if (scooter.getStatus() == null || scooter.getLockStatus() == null || scooter.getRentalMode() == null) {
            return false;
        }

        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(rentalMode)) {
            Store store = resolveStore(scooter.getStoreId());
            if (store == null) {
                return false;
            }
            applyStoreSnapshot(scooter, store);
        } else {
            if (!applyScanRideSnapshot(scooter, scooter)) {
                return false;
            }
        }
        return baseMapper.insert(scooter) > 0;
    }

    @Override
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean updateScooter(Scooter scooter) {
        if (scooter == null || scooter.getId() == null) {
            return false;
        }
        Scooter existing = baseMapper.selectById(scooter.getId());
        if (existing == null) {
            return false;
        }
        boolean inUseBeforeUpdate = RentalConstants.SCOOTER_STATUS_IN_USE.equals(existing.getStatus());

        if (scooter.getScooterCode() != null) {
            String normalizedCode = scooter.getScooterCode().trim();
            if (normalizedCode.isBlank() || !isUniqueScooterCode(normalizedCode, scooter.getId())) {
                return false;
            }
            existing.setScooterCode(normalizedCode);
        }

        if (scooter.getStatus() != null) {
            String normalizedStatus = normalizeScooterStatus(scooter.getStatus());
            if (normalizedStatus == null) {
                return false;
            }
            existing.setStatus(normalizedStatus);
        }

        if (scooter.getLockStatus() != null) {
            String normalizedLockStatus = normalizeLockStatus(scooter.getLockStatus());
            if (normalizedLockStatus == null) {
                return false;
            }
            existing.setLockStatus(normalizedLockStatus);
        }

        String targetRentalMode = scooter.getRentalMode() == null
                ? normalizeRentalMode(existing.getRentalMode())
                : normalizeRentalMode(scooter.getRentalMode());
        if (targetRentalMode == null) {
            return false;
        }
        if (isProtectedOperationalUpdate(inUseBeforeUpdate, existing, scooter, targetRentalMode)) {
            return false;
        }
        existing.setRentalMode(targetRentalMode);

        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(targetRentalMode)) {
            Long targetStoreId = scooter.getStoreId() != null ? scooter.getStoreId() : existing.getStoreId();
            Store store = resolveStore(targetStoreId);
            if (store == null) {
                return false;
            }
            applyStoreSnapshot(existing, store);
        } else {
            if (!applyScanRideSnapshot(existing, scooter)) {
                return false;
            }
        }

        return baseMapper.updateById(existing) > 0;
    }

    @Override
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean deleteScooter(Long id) {
        Scooter scooter = baseMapper.selectById(id);
        if (scooter == null || RentalConstants.SCOOTER_STATUS_IN_USE.equals(scooter.getStatus())) {
            return false;
        }
        Long openBookingCount = bookingMapper.selectCount(
                new QueryWrapper<Booking>()
                        .eq("scooter_id", id)
                        .in("status",
                                RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                                RentalConstants.BOOKING_STATUS_OVERDUE)
        );
        if (openBookingCount != null && openBookingCount > 0) {
            return false;
        }
        return baseMapper.deleteById(id) > 0;
    }

    private void enrichScooters(List<Scooter> scooters) {
        if (scooters == null || scooters.isEmpty()) {
            return;
        }
        Map<Long, Store> storeMap = loadStoreMap(scooters);
        for (Scooter scooter : scooters) {
            if (RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(normalizeRentalMode(scooter.getRentalMode()))
                    || scooter.getStoreId() == null) {
                scooter.setStoreName(null);
                scooter.setStoreAddress(null);
                continue;
            }
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

    private Map<Long, Store> loadStoreMap(List<Scooter> scooters) {
        List<Long> storeIds = scooters.stream()
                .map(Scooter::getStoreId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Store> storeMap = new HashMap<>();
        storeMapper.selectBatchIds(storeIds).forEach(store -> storeMap.put(store.getId(), store));
        return storeMap;
    }

    private boolean isUniqueScooterCode(String scooterCode, Long excludeId) {
        LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scooter::getScooterCode, scooterCode);
        if (excludeId != null) {
            wrapper.ne(Scooter::getId, excludeId);
        }
        Long count = baseMapper.selectCount(wrapper);
        return count == null || count == 0;
    }

    private Store resolveStore(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return storeMapper.selectById(storeId);
    }

    private void applyStoreSnapshot(Scooter scooter, Store store) {
        scooter.setRentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP);
        scooter.setStoreId(store.getId());
        scooter.setLocation(store.getAddress());
        scooter.setLongitude(store.getLongitude());
        scooter.setLatitude(store.getLatitude());
        scooter.setStoreName(store.getName());
        scooter.setStoreAddress(store.getAddress());
    }

    private boolean applyScanRideSnapshot(Scooter target, Scooter source) {
        String previousRentalMode = normalizeRentalMode(target.getRentalMode());
        BigDecimal longitude = source.getLongitude() != null ? source.getLongitude() : target.getLongitude();
        BigDecimal latitude = source.getLatitude() != null ? source.getLatitude() : target.getLatitude();
        if (!hasValidCoordinates(longitude, latitude)) {
            return false;
        }
        target.setRentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE);
        target.setStoreId(null);
        target.setStoreName(null);
        target.setStoreAddress(null);
        target.setLongitude(longitude);
        target.setLatitude(latitude);
        if (source.getLocation() != null) {
            String normalizedLocation = source.getLocation().trim();
            target.setLocation(normalizedLocation.isBlank() ? null : normalizedLocation);
        } else if (!RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(previousRentalMode)) {
            target.setLocation(null);
        }
        return true;
    }

    private String normalizeScooterStatus(String status) {
        if (status == null || status.isBlank()) {
            return RentalConstants.SCOOTER_STATUS_AVAILABLE;
        }
        String normalized = status.trim().toUpperCase();
        return VALID_SCOOTER_STATUSES.contains(normalized) ? normalized : null;
    }

    private String normalizeLockStatus(String lockStatus) {
        if (lockStatus == null || lockStatus.isBlank()) {
            return RentalConstants.SCOOTER_LOCK_STATUS_LOCKED;
        }
        String normalized = lockStatus.trim().toUpperCase();
        return VALID_LOCK_STATUSES.contains(normalized) ? normalized : null;
    }

    private String normalizeRentalMode(String rentalMode) {
        if (rentalMode == null || rentalMode.isBlank()) {
            return RentalConstants.RENTAL_TYPE_STORE_PICKUP;
        }
        String normalized = rentalMode.trim().toUpperCase();
        return VALID_RENTAL_MODES.contains(normalized) ? normalized : null;
    }

    private boolean hasValidCoordinates(java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        return isBetween(longitude, new java.math.BigDecimal("-180"), new java.math.BigDecimal("180"))
                && isBetween(latitude, new java.math.BigDecimal("-90"), new java.math.BigDecimal("90"));
    }

    private boolean isBetween(java.math.BigDecimal value,
                              java.math.BigDecimal min,
                              java.math.BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private boolean isProtectedOperationalUpdate(boolean inUseBeforeUpdate,
                                                 Scooter existing,
                                                 Scooter update,
                                                 String targetRentalMode) {
        if (!inUseBeforeUpdate) {
            return false;
        }
        if (update.getRentalMode() != null && !Objects.equals(normalizeRentalMode(existing.getRentalMode()), targetRentalMode)) {
            return true;
        }
        if (update.getStoreId() != null && !Objects.equals(update.getStoreId(), existing.getStoreId())) {
            return true;
        }
        return update.getLongitude() != null
                || update.getLatitude() != null
                || update.getLocation() != null;
    }
}
