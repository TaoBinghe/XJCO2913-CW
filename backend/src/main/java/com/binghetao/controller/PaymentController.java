package com.binghetao.controller;

import com.binghetao.domain.Payment;
import com.binghetao.domain.Result;
import com.binghetao.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Simulate payment for a booking by its ID.
     * Requires login (JWT token in Authorization header); no password or real card details needed.
     */
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
