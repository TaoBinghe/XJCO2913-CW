package com.greengo.domain;

import com.baomidou.mybatisplus.annotation.TableField;
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

    private Long storeId;

    private String rentalType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime pickupDeadline;

    private LocalDateTime pickupTime;

    private LocalDateTime returnTime;

    private String pickupLocation;

    private BigDecimal pickupLongitude;

    private BigDecimal pickupLatitude;

    private String returnLocation;

    private BigDecimal returnLongitude;

    private BigDecimal returnLatitude;

    private BigDecimal totalCost;

    private BigDecimal overdueCost;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String storeAddress;

    @TableField(exist = false)
    private BigDecimal storeLongitude;

    @TableField(exist = false)
    private BigDecimal storeLatitude;

    @TableField(exist = false)
    private String scooterCode;

    @TableField(exist = false)
    private String scooterStatus;

    @TableField(exist = false)
    private String lockStatus;

    @TableField(exist = false)
    private String hirePeriod;
}

