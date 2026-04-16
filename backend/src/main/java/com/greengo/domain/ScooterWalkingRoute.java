package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScooterWalkingRoute {

    private Long scooterId;

    private String scooterCode;

    private BigDecimal fromLongitude;

    private BigDecimal fromLatitude;

    private BigDecimal toLongitude;

    private BigDecimal toLatitude;

    private Long distanceMeters;

    private Long durationSeconds;

    private String routeMode;

    private List<MapRoutePoint> points;
}
