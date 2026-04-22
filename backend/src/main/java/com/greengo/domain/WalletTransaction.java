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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private Long bookingId;

    private Long bankCardId;

    private LocalDateTime createdAt;
}
