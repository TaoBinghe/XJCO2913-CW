package com.greengo.service;

import com.greengo.domain.Booking;
import com.greengo.domain.BookingSettlementResult;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface BookingService {

    List<PricingPlan> listPricingPlan();

    Booking createStoreBooking(Long storeId, LocalDateTime appointmentStart, String hiredPeriod);

    Booking cancelStoreBooking(Long bookingId);

    Booking startScanRide(String scooterCode);

    List<Scooter> listPickupScooters(Long bookingId);

    Booking pickupBooking(Long bookingId, Long scooterId);

    Booking lockScooter(Long bookingId);

    Booking unlockScooter(Long bookingId);

    BookingSettlementResult returnBooking(Long bookingId);

    BookingSettlementResult returnScanRide(Long bookingId, BigDecimal longitude, BigDecimal latitude);

    int expireReservations();

    int markOverdueBookings();

    // Legacy direct-booking API kept for compilation compatibility.
    boolean bookScooter(Integer scooterId, String hiredPeriod);

    boolean updateBookingStatus(Long bookingId, String status);

    boolean activateBooking(Long bookingId);

    Booking modifyBookingPeriod(Long bookingId, String hiredPeriod);

    Booking cancelBooking(Long bookingId);

    Booking renewBooking(Long bookingId, String hiredPeriod);

    Map<String, Object> finishBooking(Long bookingId);

    List<Booking> listBookingsByUserId(Long userId);
}

