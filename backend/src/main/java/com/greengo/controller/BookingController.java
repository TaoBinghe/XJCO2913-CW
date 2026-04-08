package com.binghetao.controller;

import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import com.binghetao.domain.Result;
import com.binghetao.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Booking API: list plans, book, activate
@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // List all pricing plans
    @GetMapping
    public Result<List<PricingPlan>> listPricingPlan() {
        List<PricingPlan> pricingPlans = bookingService.listPricingPlan();
        return Result.success(pricingPlans);
    }

    // Create booking for scooter and period
    @PostMapping
    public Result<?> bookScooter(@RequestParam Integer scooterId, @RequestParam String hiredPeriod) {
        boolean success = bookingService.bookScooter(scooterId, hiredPeriod);
        if (success) {
            return Result.success();
        }
        return Result.error("Scooter is not available for the requested period");
    }

    // Activate booking after user confirms
    @PostMapping("/activate")
    public Result<?> activateBooking(@RequestParam Long bookingId) {
        boolean success = bookingService.activateBooking(bookingId);
        if (success) {
            return Result.success();
        }
        return Result.error("Cannot activate this booking");
    }

    // Cancel a pending booking before it becomes active
    @PostMapping("/cancel")
    public Result<?> cancelBooking(@RequestParam Long bookingId) {
        try {
            Booking booking = bookingService.cancelBooking(bookingId);
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // Finish an active booking and complete payment
    @PostMapping("/finish")
    public Result<?> finishBooking(@RequestParam Long bookingId) {
        try {
            Map<String, Object> result = bookingService.finishBooking(bookingId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

}

