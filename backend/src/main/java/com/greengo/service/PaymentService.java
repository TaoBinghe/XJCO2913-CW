package com.greengo.service;

import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;

// Simulated payment for booking, no real card
public interface PaymentService {

    Payment pay(Long bookingId);

    Payment pay(PaymentRequest request);
}

