package com.greengo.controller;

import com.greengo.domain.Booking;
import com.greengo.domain.Result;
import com.greengo.domain.UnregisteredBookingCreateRequest;
import com.greengo.service.BookingService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/unregistered")
    public Result<?> createUnregisteredBooking(@RequestBody UnregisteredBookingCreateRequest request) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        if (request == null) {
            return Result.error("Booking request is missing");
        }
        try {
            Booking booking = bookingService.createStoreBookingForUnregistered(
                    request.getCustomerName(),
                    request.getCustomerEmail(),
                    request.getStoreId(),
                    request.getAppointmentStart(),
                    request.getHiredPeriod()
            );
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
