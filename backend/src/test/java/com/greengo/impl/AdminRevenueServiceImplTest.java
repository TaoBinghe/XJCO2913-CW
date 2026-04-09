package com.greengo.impl;

import com.greengo.domain.AdminWeeklyRevenueBucket;
import com.greengo.domain.AdminWeeklyRevenueSummary;
import com.greengo.mapper.PaymentMapper;
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

    private com.greengo.service.impl.AdminRevenueServiceImpl adminRevenueService;

    @BeforeEach
    void setUp() {
        adminRevenueService = new com.greengo.service.impl.AdminRevenueServiceImpl();
        ReflectionTestUtils.setField(adminRevenueService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(adminRevenueService, "clock",
                Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void getWeeklyRevenueSummaryReturnsAllBucketsInFixedOrder() {
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
        assertEquals("WEEK_1", summary.getBuckets().get(3).getHirePeriod());
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

        AdminWeeklyRevenueSummary summary = adminRevenueService.getWeeklyRevenueSummary();

        assertEquals("HOUR_4", summary.getMostPopularHirePeriod());
    }

    @Test
    void getWeeklyRevenueSummaryReturnsNullMostPopularWhenThereIsNoRevenue() {
        when(paymentMapper.selectWeeklyRevenueBuckets(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AdminWeeklyRevenueSummary summary = adminRevenueService.getWeeklyRevenueSummary();

        assertNull(summary.getMostPopularHirePeriod());
        assertEquals(4, summary.getBuckets().size());
        assertEquals(0L, summary.getBuckets().get(0).getOrderCount());
        assertEquals(BigDecimal.ZERO, summary.getBuckets().get(0).getTotalRevenue());
    }
}

