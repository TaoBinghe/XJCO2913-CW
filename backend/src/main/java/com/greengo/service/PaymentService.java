package com.greengo.service;

import com.greengo.domain.Payment;

// Simulated payment for booking, no real card
public interface PaymentService {

    // Pay for booking by id, returns payment record
    Payment pay(Long bookingId);
}

