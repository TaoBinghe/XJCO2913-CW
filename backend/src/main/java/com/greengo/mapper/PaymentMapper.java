package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.AdminWeeklyRevenueBucket;
import com.greengo.domain.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("""
            SELECT pp.hire_period AS hirePeriod,
                   COUNT(*) AS orderCount,
                   COALESCE(SUM(p.amount), 0) AS totalRevenue
            FROM payment p
            INNER JOIN booking b ON p.booking_id = b.id
            INNER JOIN pricing_plan pp ON b.pricing_plan_id = pp.id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time >= #{windowStart}
              AND p.payment_time < #{windowEnd}
            GROUP BY pp.hire_period
            """)
    List<AdminWeeklyRevenueBucket> selectWeeklyRevenueBuckets(@Param("windowStart") LocalDateTime windowStart,
                                                             @Param("windowEnd") LocalDateTime windowEnd);
}

