package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.ScooterWalkingRoute;
import com.greengo.service.RoutePlanningService;
import com.greengo.service.ScooterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScooterControllerTest {

    private final ScooterService scooterService = mock(ScooterService.class);
    private final RoutePlanningService routePlanningService = mock(RoutePlanningService.class);

    private ScooterController scooterController;

    @BeforeEach
    void setUp() {
        scooterController = new ScooterController();
        ReflectionTestUtils.setField(scooterController, "scooterService", scooterService);
        ReflectionTestUtils.setField(scooterController, "routePlanningService", routePlanningService);
    }

    @Test
    void routeReturnsSuccessPayload() {
        ScooterWalkingRoute route = ScooterWalkingRoute.builder()
                .scooterId(1L)
                .scooterCode("SC001")
                .distanceMeters(320L)
                .durationSeconds(280L)
                .routeMode("WALKING")
                .points(List.of())
                .build();
        when(routePlanningService.planWalkingRouteToScooter(
                1L,
                new BigDecimal("103.980000"),
                new BigDecimal("30.767000")
        )).thenReturn(route);

        Result<?> result = scooterController.route(
                1L,
                new BigDecimal("103.980000"),
                new BigDecimal("30.767000")
        );

        assertEquals(0, result.getCode());
        assertEquals(route, result.getData());
    }

    @Test
    void routeReturnsBusinessError() {
        when(routePlanningService.planWalkingRouteToScooter(
                2L,
                new BigDecimal("103.980000"),
                new BigDecimal("30.767000")
        )).thenThrow(new IllegalArgumentException("Scooter not found"));

        Result<?> result = scooterController.route(
                2L,
                new BigDecimal("103.980000"),
                new BigDecimal("30.767000")
        );

        assertEquals(1, result.getCode());
        assertEquals("Scooter not found", result.getMessage());
    }
}
