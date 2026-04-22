package com.greengo.domain;

import lombok.Data;

@Data
public class PaymentRequest {

    private Long bookingId;

    private String paymentMethod;

    private Long cardId;

    private String cardPassword;
}
