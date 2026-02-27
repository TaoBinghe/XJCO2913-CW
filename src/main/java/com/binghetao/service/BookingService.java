package com.binghetao.service;

import com.binghetao.domain.PricingPlan;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BookingService {
    List<PricingPlan> listPricingPlan();
}
