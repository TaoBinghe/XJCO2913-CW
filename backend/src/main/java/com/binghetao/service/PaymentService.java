package com.binghetao.service;

import com.binghetao.domain.Payment;

/**
 * Simulated payment: no real card or password.
 * Records payment for a booking and returns the payment record.
 */
public interface PaymentService {

    /**
     * Simulate payment for the given booking.
     * Uses current user from token; amount from booking.totalCost.
     *
     * @param bookingId the booking to pay for
     * @return the created payment record if success
     * @throws IllegalArgumentException if booking not found, not owner, already paid, or not PENDING
     */
    Payment pay(Long bookingId);
}
