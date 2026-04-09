package com.binghetao.service;

import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Booking service: pricing plans, book scooter, switch booking status, list by user
@Service
public interface BookingService {

    // List all pricing plans
    List<PricingPlan> listPricingPlan();

    // Book scooter for given period
    boolean bookScooter(Integer scooterId, String hiredPeriod);

    // Switch booking status between PENDING and ACTIVATED
    boolean updateBookingStatus(Long bookingId, String status);

    // Activate a pending booking
    boolean activateBooking(Long bookingId);

    // Cancel a pending booking owned by the current user
    Booking cancelBooking(Long bookingId);

    // Finish an active booking, pay for it, and return the updated booking and payment
    Map<String, Object> finishBooking(Long bookingId);

    // List bookings by user id, newest first
    List<Booking> listBookingsByUserId(Long userId);
}
