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
 * E-scooter entity.
 * Supports vehicle management, status and location info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("scooter")
public class Scooter {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Business-unique code, e.g. SC001 */
    private String scooterCode;

    /** Status: AVAILABLE / UNAVAILABLE */
    private String status;

    private Long storeId;

    private String rentalMode;

    /** Lock status: LOCKED / UNLOCKED */
    private String lockStatus;

    /** Location name for map display */
    private String location;

    /** GCJ-02 longitude for AMap */
    private BigDecimal longitude;

    /** GCJ-02 latitude for AMap */
    private BigDecimal latitude;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String storeAddress;

    @TableField(exist = false)
    private Integer batteryLevel;

    @TableField(exist = false)
    private BigDecimal remainingRangeKm;
}

