package com.binghetao.controller;

import com.binghetao.domain.PricingPlan;
import com.binghetao.domain.Result;
import com.binghetao.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public Result<List<PricingPlan>> listPricingPlan() {
        List<PricingPlan> pricingPlans = bookingService.listPricingPlan();
        return Result.success(pricingPlans);
    }


}
