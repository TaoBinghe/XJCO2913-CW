package com.greengo.service.impl;

import com.greengo.domain.AdminWeeklyRevenueBucket;
import com.greengo.domain.AdminWeeklyRevenueSummary;
import com.greengo.mapper.PaymentMapper;
import com.greengo.service.AdminRevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminRevenueServiceImpl implements AdminRevenueService {

    private static final List<String> HIRE_PERIOD_ORDER = List.of("HOUR_1", "HOUR_4", "DAY_1", "WEEK_1");

    @Autowired
    private PaymentMapper paymentMapper;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public AdminWeeklyRevenueSummary getWeeklyRevenueSummary() {
        LocalDateTime windowEnd = LocalDateTime.now(clock);
        LocalDateTime windowStart = windowEnd.minusDays(7);

        List<AdminWeeklyRevenueBucket> aggregatedBuckets = paymentMapper.selectWeeklyRevenueBuckets(windowStart, windowEnd);
        Map<String, AdminWeeklyRevenueBucket> bucketMap = new HashMap<>();
        if (aggregatedBuckets != null) {
            aggregatedBuckets.forEach(bucket -> bucketMap.put(bucket.getHirePeriod(), normalizeBucket(bucket)));
        }

        List<AdminWeeklyRevenueBucket> buckets = new ArrayList<>();
        for (String hirePeriod : HIRE_PERIOD_ORDER) {
            buckets.add(bucketMap.getOrDefault(hirePeriod, zeroBucket(hirePeriod)));
        }

        return AdminWeeklyRevenueSummary.builder()
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .buckets(buckets)
                .mostPopularHirePeriod(resolveMostPopularHirePeriod(buckets))
                .build();
    }

    private AdminWeeklyRevenueBucket normalizeBucket(AdminWeeklyRevenueBucket bucket) {
        return AdminWeeklyRevenueBucket.builder()
                .hirePeriod(bucket.getHirePeriod())
                .orderCount(bucket.getOrderCount() == null ? 0L : bucket.getOrderCount())
                .totalRevenue(bucket.getTotalRevenue() == null ? BigDecimal.ZERO : bucket.getTotalRevenue())
                .build();
    }

    private AdminWeeklyRevenueBucket zeroBucket(String hirePeriod) {
        return AdminWeeklyRevenueBucket.builder()
                .hirePeriod(hirePeriod)
                .orderCount(0L)
                .totalRevenue(BigDecimal.ZERO)
                .build();
    }

    private String resolveMostPopularHirePeriod(List<AdminWeeklyRevenueBucket> buckets) {
        AdminWeeklyRevenueBucket best = null;
        for (AdminWeeklyRevenueBucket bucket : buckets) {
            if (bucket.getOrderCount() == null || bucket.getOrderCount() <= 0) {
                continue;
            }
            if (best == null) {
                best = bucket;
                continue;
            }
            if (bucket.getOrderCount() > best.getOrderCount()) {
                best = bucket;
                continue;
            }
            if (bucket.getOrderCount().equals(best.getOrderCount())
                    && bucket.getTotalRevenue().compareTo(best.getTotalRevenue()) > 0) {
                best = bucket;
            }
        }
        return best == null ? null : best.getHirePeriod();
    }
}

