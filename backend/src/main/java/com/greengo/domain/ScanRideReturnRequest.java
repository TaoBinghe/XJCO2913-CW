package com.greengo.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScanRideReturnRequest {

    private BigDecimal longitude;

    private BigDecimal latitude;
}
