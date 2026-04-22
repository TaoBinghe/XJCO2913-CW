package com.greengo.domain;

import lombok.Data;

@Data
public class BankCardBindRequest {

    private String bankName;

    private String holderName;

    private String cardNumber;

    private String cardPassword;
}
