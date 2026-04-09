package com.greengo.service;

import com.greengo.domain.PricingPlan;

import java.util.List;

/**
 * Admin pricing plan CRUD service.
 */
public interface PricingPlanService {

    List<PricingPlan> listAll();

    PricingPlan getById(Long id);

    /**
     * Create a new pricing plan. hirePeriod must be unique, price must be > 0.
     */
    boolean create(PricingPlan plan);

    /**
     * Update by id. If hirePeriod is changed, must remain unique. price if present must be > 0.
     */
    boolean update(Long id, PricingPlan plan);

    /**
     * Delete by id. Fails if any booking references this plan.
     * @return true if deleted, false if not found or in use (check message via exception or return value)
     */
    boolean delete(Long id);

    /**
     * Whether any booking uses this pricing plan id.
     */
    boolean isUsedByBooking(Long pricingPlanId);
}

