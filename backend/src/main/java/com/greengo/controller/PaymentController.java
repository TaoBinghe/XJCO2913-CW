package com.greengo.controller;

import com.greengo.domain.Payment;
import com.greengo.domain.PaymentRequest;
import com.greengo.domain.Result;
import com.greengo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public Result<?> pay(@RequestBody PaymentRequest request) {
        if (request == null || request.getBookingId() == null || request.getPaymentMethod() == null) {
            return Result.error("Booking id and payment method are required");
        }
        try {
            Payment payment = paymentService.pay(request);
            return Result.success(payment);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}

