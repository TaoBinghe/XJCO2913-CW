package com.greengo.impl;

import com.greengo.domain.AdminDailyRevenueBucket;
import com.greengo.domain.AdminDailyRevenueSummary;
import com.greengo.domain.AdminWeeklyRevenueBucket;
import com.greengo.domain.AdminWeeklyRevenueSummary;
import com.greengo.mapper.PaymentMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.domain.PricingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRevenueServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PricingPlanMapper pricingPlanMapper;

    private com.greengo.service.impl.AdminRevenueServiceImpl adminRevenueService;

    @BeforeEach
    void setUp() {
        adminRevenueService = new com.greengo.service.impl.AdminRevenueServiceImpl();
        ReflectionTestUtils.setField(adminRevenueService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(adminRevenueService, "pricingPlanMapper", pricingPlanMapper);
        ReflectionTestUtils.setField(adminRevenueService, "clock",
                Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void getWeeklyRevenueSummaryReturnsAllBucketsInDurationOrder() {
        when(paymentMapper.selectWeeklyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        AdminWeeklyRevenueBucket.builder()
                                .hirePeriod("HOUR_1")
                                .orderCount(2L)
                                .totalRevenue(new BigDecimal("10.00"))
                                .build(),
                        AdminWeeklyRevenueBucket.builder()
                                .hirePeriod("DAY_1")
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("30.00"))
                                .build()
                ));
        when(pricingPlanMapper.selectList(org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(
                        PricingPlan.builder().hirePeriod("HOUR_1").build(),
                        PricingPlan.builder().hirePeriod("HOUR_4").build(),
                        PricingPlan.builder().hirePeriod("DAY_1").build(),
                        PricingPlan.builder().hirePeriod("DAY_3").build()
                ));

        AdminWeeklyRevenueSummary summary = adminRevenueService.getWeeklyRevenueSummary();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(paymentMapper).selectWeeklyRevenueBuckets(startCaptor.capture(), endCaptor.capture());

        assertEquals(LocalDateTime.of(2026, 4, 2, 12, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 4, 9, 12, 0), endCaptor.getValue());
        assertEquals("HOUR_1", summary.getMostPopularHirePeriod());
        assertEquals(4, summary.getBuckets().size());
        assertEquals("HOUR_1", summary.getBuckets().get(0).getHirePeriod());
        assertEquals(2L, summary.getBuckets().get(0).getOrderCount());
        assertEquals(new BigDecimal("10.00"), summary.getBuckets().get(0).getTotalRevenue());
        assertEquals("HOUR_4", summary.getBuckets().get(1).getHirePeriod());
        assertEquals(0L, summary.getBuckets().get(1).getOrderCount());
        assertEquals(BigDecimal.ZERO, summary.getBuckets().get(1).getTotalRevenue());
        assertEquals("DAY_1", summary.getBuckets().get(2).getHirePeriod());
        assertEquals(1L, summary.getBuckets().get(2).getOrderCount());
        assertEquals(new BigDecimal("30.00"), summary.getBuckets().get(2).getTotalRevenue());
        assertEquals("DAY_3", summary.getBuckets().get(3).getHirePeriod());
        assertEquals(0L, summary.getBuckets().get(3).getOrderCount());
    }

    @Test
    void getWeeklyRevenueSummaryUsesRevenueAsTieBreaker() {
        when(paymentMapper.selectWeeklyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        AdminWeeklyRevenueBucket.builder()
                                .hirePeriod("HOUR_1")
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("5.00"))
                                .build(),
                        AdminWeeklyRevenueBucket.builder()
                                .hirePeriod("HOUR_4")
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("15.00"))
                                .build()
                ));
        when(pricingPlanMapper.selectList(org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(
                        PricingPlan.builder().hirePeriod("HOUR_1").build(),
                        PricingPlan.builder().hirePeriod("HOUR_4").build()
                ));

        AdminWeeklyRevenueSummary summary = adminRevenueService.getWeeklyRevenueSummary();

        assertEquals("HOUR_4", summary.getMostPopularHirePeriod());
    }

    @Test
    void getWeeklyRevenueSummaryReturnsNullMostPopularWhenThereIsNoRevenue() {
        when(paymentMapper.selectWeeklyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(pricingPlanMapper.selectList(org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(
                        PricingPlan.builder().hirePeriod("HOUR_1").build(),
                        PricingPlan.builder().hirePeriod("HOUR_4").build()
                ));

        AdminWeeklyRevenueSummary summary = adminRevenueService.getWeeklyRevenueSummary();

        assertNull(summary.getMostPopularHirePeriod());
        assertEquals(2, summary.getBuckets().size());
        assertEquals(0L, summary.getBuckets().get(0).getOrderCount());
        assertEquals(BigDecimal.ZERO, summary.getBuckets().get(0).getTotalRevenue());
    }

    @Test
    void getDailyRevenueSummaryReturnsSevenCalendarDaysWithZeroBuckets() {
        when(paymentMapper.selectDailyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        AdminDailyRevenueBucket.builder()
                                .revenueDate(LocalDate.of(2026, 4, 4))
                                .orderCount(2L)
                                .totalRevenue(new BigDecimal("45.00"))
                                .build(),
                        AdminDailyRevenueBucket.builder()
                                .revenueDate(LocalDate.of(2026, 4, 8))
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("100.00"))
                                .build()
                ));

        AdminDailyRevenueSummary summary = adminRevenueService.getDailyRevenueSummary();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(paymentMapper).selectDailyRevenueBuckets(startCaptor.capture(), endCaptor.capture());

        assertEquals(LocalDateTime.of(2026, 4, 3, 0, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 4, 10, 0, 0), endCaptor.getValue());
        assertEquals(LocalDate.of(2026, 4, 3), summary.getWindowStartDate());
        assertEquals(LocalDate.of(2026, 4, 9), summary.getWindowEndDate());
        assertEquals(7, summary.getBuckets().size());
        assertEquals(LocalDate.of(2026, 4, 3), summary.getBuckets().get(0).getRevenueDate());
        assertEquals(0L, summary.getBuckets().get(0).getOrderCount());
        assertEquals(BigDecimal.ZERO, summary.getBuckets().get(0).getTotalRevenue());
        assertEquals(LocalDate.of(2026, 4, 4), summary.getBuckets().get(1).getRevenueDate());
        assertEquals(2L, summary.getBuckets().get(1).getOrderCount());
        assertEquals(new BigDecimal("45.00"), summary.getBuckets().get(1).getTotalRevenue());
        assertEquals(LocalDate.of(2026, 4, 8), summary.getBuckets().get(5).getRevenueDate());
        assertEquals(new BigDecimal("145.00"), summary.getTotalRevenue());
        assertEquals(LocalDate.of(2026, 4, 4), summary.getMostPopularRevenueDate());
    }

    @Test
    void getDailyRevenueSummaryUsesRevenueAsTieBreakerForPopularDate() {
        when(paymentMapper.selectDailyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        AdminDailyRevenueBucket.builder()
                                .revenueDate(LocalDate.of(2026, 4, 5))
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("30.00"))
                                .build(),
                        AdminDailyRevenueBucket.builder()
                                .revenueDate(LocalDate.of(2026, 4, 6))
                                .orderCount(1L)
                                .totalRevenue(new BigDecimal("100.00"))
                                .build()
                ));

        AdminDailyRevenueSummary summary = adminRevenueService.getDailyRevenueSummary();

        assertEquals(LocalDate.of(2026, 4, 6), summary.getMostPopularRevenueDate());
    }
}

