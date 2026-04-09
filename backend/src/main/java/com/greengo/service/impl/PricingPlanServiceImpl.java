package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greengo.domain.Booking;
import com.greengo.domain.PricingPlan;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.service.PricingPlanService;
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
    public List<PricingPlan> listAll() {
        return pricingPlanMapper.selectList(null);
    }

    @Override
    public PricingPlan getById(Long id) {
        return pricingPlanMapper.selectById(id);
    }

    @Override
    public boolean create(PricingPlan plan) {
        if (plan == null || plan.getHirePeriod() == null || plan.getHirePeriod().isBlank()) {
            return false;
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        LambdaQueryWrapper<PricingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PricingPlan::getHirePeriod, plan.getHirePeriod());
        Long count = pricingPlanMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return false;
        }
        return pricingPlanMapper.insert(plan) > 0;
    }

    @Override
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
            LambdaQueryWrapper<PricingPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PricingPlan::getHirePeriod, plan.getHirePeriod()).ne(PricingPlan::getId, id);
            Long count = pricingPlanMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                return false;
            }
            existing.setHirePeriod(plan.getHirePeriod());
        }
        if (plan.getPrice() != null) {
            existing.setPrice(plan.getPrice());
        }
        return pricingPlanMapper.updateById(existing) > 0;
    }

    @Override
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

