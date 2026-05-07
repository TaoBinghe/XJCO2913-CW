package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDailyRevenueSummary {

    private LocalDate windowStartDate;

    private LocalDate windowEndDate;

    private List<AdminDailyRevenueBucket> buckets;

    private BigDecimal totalRevenue;

    private LocalDate mostPopularRevenueDate;
}
