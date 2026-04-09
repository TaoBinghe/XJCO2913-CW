package com.greengo.controller;

import com.greengo.domain.Booking;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Result;
import com.greengo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        try {
            bookingService.bookScooter(scooterId, hiredPeriod);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
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
        try {
            bookingService.activateBooking(bookingId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // Change the hire period of a pending booking
    @PostMapping("/modify-period")
    public Result<?> modifyBookingPeriod(@RequestParam Long bookingId, @RequestParam String hiredPeriod) {
        try {
            Booking booking = bookingService.modifyBookingPeriod(bookingId, hiredPeriod);
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
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

    // Extend an active booking by another fixed hire period
    @PostMapping("/renew")
    public Result<?> renewBooking(@RequestParam Long bookingId, @RequestParam String hiredPeriod) {
        try {
            Booking booking = bookingService.renewBooking(bookingId, hiredPeriod);
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


