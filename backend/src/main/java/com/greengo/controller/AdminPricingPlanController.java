package com.greengo.controller;

import com.greengo.domain.PricingPlan;
import com.greengo.domain.Result;
import com.greengo.service.PricingPlanService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/pricing-plans")
public class AdminPricingPlanController {

    @Autowired
    private PricingPlanService pricingPlanService;

    @GetMapping
    public Result<List<PricingPlan>> list() {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        List<PricingPlan> list = pricingPlanService.listAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<PricingPlan> getById(@PathVariable Long id) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        PricingPlan plan = pricingPlanService.getById(id);
        if (plan == null) {
            return Result.error("Pricing plan not found");
        }
        return Result.success(plan);
    }

    @PostMapping
    public Result<?> create(@RequestBody PricingPlan plan) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        if (plan.getHirePeriod() == null || plan.getHirePeriod().isBlank()) {
            return Result.error("Hire period cannot be blank");
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.error("Price must be greater than 0");
        }
        boolean ok = pricingPlanService.create(plan);
        if (ok) {
            return Result.success();
        }
        return Result.error("Hire period already exists; use a different hire period code");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody PricingPlan plan) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        if (plan.getPrice() != null && plan.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.error("Price must be greater than 0");
        }
        boolean ok = pricingPlanService.update(id, plan);
        if (ok) {
            return Result.success();
        }
        PricingPlan existing = pricingPlanService.getById(id);
        if (existing == null) {
            return Result.error("Pricing plan not found");
        }
        return Result.error("Update failed; hire period may duplicate another pricing plan");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        if (pricingPlanService.getById(id) == null) {
            return Result.error("Pricing plan not found");
        }
        if (pricingPlanService.isUsedByBooking(id)) {
            return Result.error("Pricing plan is used by existing bookings and cannot be deleted");
        }
        boolean ok = pricingPlanService.delete(id);
        if (ok) {
            return Result.success();
        }
        return Result.error("Delete failed");
    }
}
