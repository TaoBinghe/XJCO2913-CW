package com.binghetao.controller;

import com.binghetao.domain.PricingPlan;
import com.binghetao.domain.Result;
import com.binghetao.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Booking API: list plans, book, switch status, activate legacy bookings
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

    // Switch booking status between ACTIVATED and PENDING
    @PostMapping("/status")
    public Result<?> updateBookingStatus(@RequestParam Long bookingId, @RequestParam String status) {
        boolean success = bookingService.updateBookingStatus(bookingId, status);
        if (success) {
            return Result.success();
        }
        return Result.error("Cannot update this booking status");
    }

    // Activate a pending booking
    @PostMapping("/activate")
    public Result<?> activateBooking(@RequestParam Long bookingId) {
        boolean success = bookingService.activateBooking(bookingId);
        if (success) {
            return Result.success();
        }
        return Result.error("Cannot activate this booking");
    }

}

