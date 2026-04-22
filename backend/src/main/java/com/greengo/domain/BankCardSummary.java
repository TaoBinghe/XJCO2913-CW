package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCardSummary {

    private Long id;

    private String bankName;

    private String holderName;

    private String maskedCardNumber;

    private String cardLastFour;
}
