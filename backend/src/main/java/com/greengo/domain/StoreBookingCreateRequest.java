package com.greengo.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreBookingCreateRequest {

    private Long storeId;

    private LocalDateTime appointmentStart;

    private String hiredPeriod;
}
