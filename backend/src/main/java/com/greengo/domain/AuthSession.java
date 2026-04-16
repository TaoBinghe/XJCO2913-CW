package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    private String sid;

    private Long userId;

    private String username;

    private String role;

    private Integer status;

    private LocalDateTime loginAt;
}
