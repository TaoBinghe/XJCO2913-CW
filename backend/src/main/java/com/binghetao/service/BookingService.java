package com.binghetao.service;

import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import org.springframework.stereotype.Service;

import java.util.List;

// Booking service: pricing plans, book scooter, activate, list by user
@Service
public interface BookingService {

    // List all pricing plans
    List<PricingPlan> listPricingPlan();

    // Book scooter for given period
    boolean bookScooter(Integer scooterId, String hiredPeriod);

    // Activate booking after user confirms
    boolean activateBooking(Long bookingId);

    // List bookings by user id, newest first
    List<Booking> listBookingsByUserId(Long userId);
}
