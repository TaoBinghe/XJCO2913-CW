package com.binghetao.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** Location name for map display */
    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
