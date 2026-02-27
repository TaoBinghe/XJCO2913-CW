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
 * User entity.
 * Supports registration, login and CUSTOMER / MANAGER role distinction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    // Role: CUSTOMER / MANAGER
    private String role;

    // Status: 0-disabled, 1-enabled
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
