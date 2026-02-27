package com.binghetao.domain;

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
 * Booking entity.
 * Links user, scooter and pricing plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("booking")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long scooterId;

    private Long pricingPlanId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal totalCost;

    // Status: PENDING / ACTIVE / COMPLETED / CANCELLED 
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
