package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.Scooter;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.GeoAddressService;
import com.greengo.service.ScooterService;
import com.greengo.utils.RedisCacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ScooterServiceImpl extends ServiceImpl<ScooterMapper, Scooter> implements ScooterService {

    private static final String SCOOTER_STATUS_AVAILABLE = "AVAILABLE";

    @Autowired
    private GeoAddressService geoAddressService;

    @Override
    @Cacheable(value = RedisCacheNames.SCOOTER_LIST, key = "'all'")
    public List<Scooter> listAll() {
        List<Scooter> scooters = baseMapper.selectList(null);
        scooters.forEach(this::hydrateLocationForRead);
        return scooters;
    }

    @Override
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean addScooter(Scooter scooter) {
        if (scooter == null || scooter.getScooterCode() == null || scooter.getScooterCode().isBlank()) {
            return false;
        }
        if (!hasValidCoordinates(scooter)) {
            return false;
        }

        LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scooter::getScooterCode, scooter.getScooterCode());
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return false;
        }

        if (scooter.getStatus() == null || scooter.getStatus().isBlank()) {
            scooter.setStatus(SCOOTER_STATUS_AVAILABLE);
        }
        if (!hydrateLocationFromCoordinates(scooter)) {
            return false;
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

        // If the scooter code changes, verify it remains unique.
        String scooterCode = scooter.getScooterCode();
        if (scooterCode != null && !scooterCode.equals(existing.getScooterCode())) {
            LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Scooter::getScooterCode, scooterCode)
                    .ne(Scooter::getId, scooter.getId());
            Long count = baseMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                return false;
            }
            existing.setScooterCode(scooterCode);
        }

        if (scooter.getStatus() != null) {
            existing.setStatus(scooter.getStatus());
        }
        if (scooter.getLocation() != null) {
            existing.setLocation(scooter.getLocation());
        }
        if (scooter.getLongitude() != null) {
            existing.setLongitude(scooter.getLongitude());
        }
        if (scooter.getLatitude() != null) {
            existing.setLatitude(scooter.getLatitude());
        }
        if (!hasValidCoordinates(existing)) {
            return false;
        }
        if (!hydrateLocationFromCoordinates(existing)) {
            return false;
        }

        return baseMapper.updateById(existing) > 0;
    }

    @Override
    @CacheEvict(value = RedisCacheNames.SCOOTER_LIST, allEntries = true)
    public boolean deleteScooter(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    private boolean hasValidCoordinates(Scooter scooter) {
        return scooter != null && hasValidCoordinates(scooter.getLongitude(), scooter.getLatitude());
    }

    private boolean hasValidCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        return isBetween(longitude, new BigDecimal("-180"), new BigDecimal("180"))
                && isBetween(latitude, new BigDecimal("-90"), new BigDecimal("90"));
    }

    private boolean hydrateLocationFromCoordinates(Scooter scooter) {
        String resolvedLocation = geoAddressService.reverseGeocode(scooter.getLongitude(), scooter.getLatitude());
        if (resolvedLocation == null || resolvedLocation.isBlank()) {
            return false;
        }
        scooter.setLocation(resolvedLocation);
        return true;
    }

    private void hydrateLocationForRead(Scooter scooter) {
        if (!hasValidCoordinates(scooter)) {
            return;
        }
        String resolvedLocation = geoAddressService.reverseGeocode(scooter.getLongitude(), scooter.getLatitude());
        if (resolvedLocation != null && !resolvedLocation.isBlank()) {
            scooter.setLocation(resolvedLocation);
        }
    }

    private boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
