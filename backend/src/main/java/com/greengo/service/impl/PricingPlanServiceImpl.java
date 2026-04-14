package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greengo.domain.Booking;
import com.greengo.domain.PricingPlan;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.service.PricingPlanService;
import com.greengo.utils.PricingPlanPeriodUtil;
import com.greengo.utils.RedisCacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingPlanServiceImpl implements PricingPlanService {

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Override
    @Cacheable(value = RedisCacheNames.PRICING_PLAN_LIST, key = "'all'")
    public List<PricingPlan> listAll() {
        return pricingPlanMapper.selectList(null);
    }

    @Override
    @Cacheable(value = RedisCacheNames.PRICING_PLAN_BY_ID, key = "#id")
    public PricingPlan getById(Long id) {
        return pricingPlanMapper.selectById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_LIST, allEntries = true),
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_BY_ID, allEntries = true),
            @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    })
    public boolean create(PricingPlan plan) {
        if (plan == null) {
            return false;
        }

        String normalizedHirePeriod = PricingPlanPeriodUtil.normalizeHirePeriod(plan.getHirePeriod());
        if (normalizedHirePeriod == null) {
            return false;
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        plan.setHirePeriod(normalizedHirePeriod);
        LambdaQueryWrapper<PricingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PricingPlan::getHirePeriod, normalizedHirePeriod);
        Long count = pricingPlanMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return false;
        }
        return pricingPlanMapper.insert(plan) > 0;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_LIST, allEntries = true),
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_BY_ID, allEntries = true),
            @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    })
    public boolean update(Long id, PricingPlan plan) {
        if (id == null || plan == null) {
            return false;
        }
        PricingPlan existing = pricingPlanMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        if (plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (plan.getHirePeriod() != null && !plan.getHirePeriod().equals(existing.getHirePeriod())) {
            String normalizedHirePeriod = PricingPlanPeriodUtil.normalizeHirePeriod(plan.getHirePeriod());
            if (normalizedHirePeriod == null) {
                return false;
            }
            LambdaQueryWrapper<PricingPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PricingPlan::getHirePeriod, normalizedHirePeriod).ne(PricingPlan::getId, id);
            Long count = pricingPlanMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                return false;
            }
            existing.setHirePeriod(normalizedHirePeriod);
        }
        if (plan.getPrice() != null) {
            existing.setPrice(plan.getPrice());
        }
        return pricingPlanMapper.updateById(existing) > 0;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_LIST, allEntries = true),
            @CacheEvict(value = RedisCacheNames.PRICING_PLAN_BY_ID, allEntries = true),
            @CacheEvict(value = RedisCacheNames.ADMIN_WEEKLY_REVENUE, allEntries = true)
    })
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        if (pricingPlanMapper.selectById(id) == null) {
            return false;
        }
        if (isUsedByBooking(id)) {
            return false;
        }
        return pricingPlanMapper.deleteById(id) > 0;
    }

    @Override
    public boolean isUsedByBooking(Long pricingPlanId) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getPricingPlanId, pricingPlanId);
        Long count = bookingMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}

