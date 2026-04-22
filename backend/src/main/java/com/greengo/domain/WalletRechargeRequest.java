package com.greengo.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRechargeRequest {

    private Long cardId;

    private String cardPassword;

    private BigDecimal amount;
}
