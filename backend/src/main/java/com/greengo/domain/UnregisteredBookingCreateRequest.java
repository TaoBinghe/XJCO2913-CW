package com.greengo.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnregisteredBookingCreateRequest {

    private String customerName;

    private String customerEmail;

    private Long storeId;

    private LocalDateTime appointmentStart;

    private String hiredPeriod;
}
