package com.greengo.service;

import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;

public interface PaymentService {

    Payment pay(PaymentRequest request);
}

