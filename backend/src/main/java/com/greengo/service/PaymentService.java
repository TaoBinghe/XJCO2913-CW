package com.greengo.service;

import com.greengo.domain.Payment;

// Simulated payment for booking, no real card
public interface PaymentService {

    Payment pay(Long bookingId);
}

