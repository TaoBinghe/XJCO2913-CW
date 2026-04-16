package com.greengo.service;

import com.greengo.domain.ScooterWalkingRoute;

import java.math.BigDecimal;

public interface RoutePlanningService {

    ScooterWalkingRoute planWalkingRouteToScooter(Long scooterId, BigDecimal fromLongitude, BigDecimal fromLatitude);
}
