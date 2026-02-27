package com.binghetao.controller;

import com.binghetao.domain.PricingPlan;
import com.binghetao.domain.Result;
import com.binghetao.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public Result<?> bookScooter(@RequestParam Integer scooterId, @RequestParam String hiredPeriod) {
        boolean success = bookingService.bookScooter(scooterId, hiredPeriod);
        if (success) {
            return Result.success();
        }
        return Result.error("Scooter is not available for the requested period");
    }

    /**
     * Activate a booking after the user confirms the selection on frontend.
     */
    @PostMapping("/activate")
    public Result<?> activateBooking(@RequestParam Long bookingId) {
        boolean success = bookingService.activateBooking(bookingId);
        if (success) {
            return Result.success();
        }
        return Result.error("Cannot activate this booking");
    }

}

