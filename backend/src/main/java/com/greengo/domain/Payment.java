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
 * Payment entity.
 * Simulated payment; records amount, date and user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private Long userId;

    private BigDecimal amount;

    /** Status: SUCCESS / FAILED */
    private String status;

    /** Simulated payment: last four digits of card */
    private String cardLastFour;

    /** Simulated transaction ID */
    private String transactionId;

    private LocalDateTime paymentTime;
}

