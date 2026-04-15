package com.greengo.controller;

import com.greengo.domain.Booking;
import com.greengo.domain.BookingPickupRequest;
import com.greengo.domain.BookingSettlementResult;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Result;
import com.greengo.domain.ScanRideReturnRequest;
import com.greengo.domain.ScanRideStartRequest;
import com.greengo.domain.Scooter;
import com.greengo.domain.StoreBookingCreateRequest;
import com.greengo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public Result<List<PricingPlan>> listPricingPlan() {
        return Result.success(bookingService.listPricingPlan());
    }

    @PostMapping
    public Result<?> createStoreBooking(@RequestBody StoreBookingCreateRequest request) {
        if (request == null) {
            return Result.error("Booking request is missing");
        }
        try {
            Booking booking = bookingService.createStoreBooking(
                    request.getStoreId(),
                    request.getAppointmentStart(),
                    request.getHiredPeriod()
            );
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scan/start")
    public Result<?> startScanRide(@RequestBody ScanRideStartRequest request) {
        if (request == null || request.getScooterCode() == null || request.getScooterCode().isBlank()) {
            return Result.error("Scooter code is required");
        }
        try {
            return Result.success(bookingService.startScanRide(request.getScooterCode()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public Result<?> cancelStoreBooking(@PathVariable Long bookingId) {
        try {
            return Result.success(bookingService.cancelStoreBooking(bookingId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{bookingId}/pickup-scooters")
    public Result<?> listPickupScooters(@PathVariable Long bookingId) {
        try {
            List<Scooter> scooters = bookingService.listPickupScooters(bookingId);
            return Result.success(scooters);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/pickup")
    public Result<?> pickupBooking(@PathVariable Long bookingId, @RequestBody BookingPickupRequest request) {
        if (request == null || request.getScooterId() == null) {
            return Result.error("Pickup scooter is required");
        }
        try {
            Booking booking = bookingService.pickupBooking(bookingId, request.getScooterId());
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/lock")
    public Result<?> lockScooter(@PathVariable Long bookingId) {
        try {
            return Result.success(bookingService.lockScooter(bookingId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scan/{bookingId}/lock")
    public Result<?> lockScanRide(@PathVariable Long bookingId) {
        return lockScooter(bookingId);
    }

    @PostMapping("/{bookingId}/unlock")
    public Result<?> unlockScooter(@PathVariable Long bookingId) {
        try {
            return Result.success(bookingService.unlockScooter(bookingId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scan/{bookingId}/unlock")
    public Result<?> unlockScanRide(@PathVariable Long bookingId) {
        return unlockScooter(bookingId);
    }

    @PostMapping("/{bookingId}/return")
    public Result<?> returnBooking(@PathVariable Long bookingId) {
        try {
            BookingSettlementResult result = bookingService.returnBooking(bookingId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scan/{bookingId}/return")
    public Result<?> returnScanRide(@PathVariable Long bookingId, @RequestBody ScanRideReturnRequest request) {
        if (request == null || request.getLongitude() == null || request.getLatitude() == null) {
            return Result.error("Return coordinates are required");
        }
        try {
            BookingSettlementResult result = bookingService.returnScanRide(
                    bookingId,
                    request.getLongitude(),
                    request.getLatitude()
            );
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/status")
    public Result<?> updateBookingStatus(@RequestParam Long bookingId, @RequestParam String status) {
        try {
            bookingService.updateBookingStatus(bookingId, status);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/activate")
    public Result<?> activateBooking(@RequestParam Long bookingId) {
        try {
            bookingService.activateBooking(bookingId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/modify-period")
    public Result<?> modifyBookingPeriod(@RequestParam Long bookingId, @RequestParam String hiredPeriod) {
        try {
            return Result.success(bookingService.modifyBookingPeriod(bookingId, hiredPeriod));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public Result<?> cancelBooking(@RequestParam Long bookingId) {
        try {
            return Result.success(bookingService.cancelBooking(bookingId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/renew")
    public Result<?> renewBooking(@RequestParam Long bookingId, @RequestParam String hiredPeriod) {
        try {
            return Result.success(bookingService.renewBooking(bookingId, hiredPeriod));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/finish")
    public Result<?> finishBooking(@RequestParam Long bookingId) {
        try {
            return Result.success(bookingService.finishBooking(bookingId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
