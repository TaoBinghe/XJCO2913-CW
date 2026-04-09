package com.greengo.controller;

import com.greengo.domain.Payment;
import com.greengo.domain.Result;
import com.greengo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Payment API: simulate pay for booking
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Simulate payment for booking by id (JWT required)
    @PostMapping
    public Result<?> pay(@RequestParam Long bookingId) {
        try {
            Payment payment = paymentService.pay(bookingId);
            return Result.success(payment);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}

