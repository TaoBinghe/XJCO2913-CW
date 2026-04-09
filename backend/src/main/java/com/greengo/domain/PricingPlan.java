package com.greengo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pricing plan entity.
 * Four hire periods: 1hr, 4hrs, 1day, 1week.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("pricing_plan")
public class PricingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;


    /** Hire period: HOUR_1 / HOUR_4 / DAY_1 / WEEK_1 */
    private String hirePeriod;

    private BigDecimal price;

    private LocalDateTime updatedAt;
}

