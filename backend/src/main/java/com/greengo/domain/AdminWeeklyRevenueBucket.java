package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminWeeklyRevenueBucket {

    private String hirePeriod;

    private Long orderCount;

    private BigDecimal totalRevenue;
}

