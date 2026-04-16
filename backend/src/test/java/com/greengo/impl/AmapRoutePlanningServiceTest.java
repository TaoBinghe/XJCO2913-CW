package com.greengo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.Scooter;
import com.greengo.domain.ScooterWalkingRoute;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.impl.AmapRoutePlanningService;
import com.greengo.utils.RentalConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmapRoutePlanningServiceTest {

    @Mock
    private ScooterMapper scooterMapper;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private AmapRoutePlanningService routePlanningService;

    @BeforeEach
    void setUp() {
        routePlanningService = new AmapRoutePlanningService(scooterMapper, httpClient, new ObjectMapper());
        ReflectionTestUtils.setField(routePlanningService, "webServiceKey", "test-key");
    }

    @Test
    void planWalkingRouteToScooterReturnsFlattenedPoints() throws Exception {
        Scooter scooter = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .status("AVAILABLE")
                .rentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .build();
        String responseBody = """
                {
                  "status":"1",
                  "route":{
                    "paths":[
                      {
                        "distance":"320",
                        "duration":"280",
                        "steps":[
                          {"polyline":"103.980000,30.767000;103.980500,30.767500"},
                          {"polyline":"103.980500,30.767500;103.981570,30.768249"}
                        ]
                      }
                    ]
                  }
                }
                """;

        when(scooterMapper.selectById(1L)).thenReturn(scooter);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn(responseBody);

        ScooterWalkingRoute route = routePlanningService.planWalkingRouteToScooter(
                1L,
                new BigDecimal("103.980000"),
                new BigDecimal("30.767000")
        );

        assertEquals(1L, route.getScooterId());
        assertEquals("SC001", route.getScooterCode());
        assertEquals("WALKING", route.getRouteMode());
        assertEquals(320L, route.getDistanceMeters());
        assertEquals(280L, route.getDurationSeconds());
        assertEquals(3, route.getPoints().size());
        assertEquals(new BigDecimal("30.767000"), route.getPoints().get(0).getLatitude());
        assertEquals(new BigDecimal("103.980000"), route.getPoints().get(0).getLongitude());
        assertEquals(new BigDecimal("30.768249"), route.getPoints().get(2).getLatitude());
    }

    @Test
    void planWalkingRouteToScooterRejectsUnavailableScooter() throws Exception {
        Scooter scooter = Scooter.builder()
                .id(1L)
                .status("UNAVAILABLE")
                .rentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .build();
        when(scooterMapper.selectById(1L)).thenReturn(scooter);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> routePlanningService.planWalkingRouteToScooter(
                        1L,
                        new BigDecimal("103.980000"),
                        new BigDecimal("30.767000")
                )
        );

        assertEquals("Scooter is not available", error.getMessage());
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void planWalkingRouteToScooterRejectsInvalidOrigin() throws Exception {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> routePlanningService.planWalkingRouteToScooter(
                        1L,
                        new BigDecimal("999"),
                        new BigDecimal("30.767000")
                )
        );

        assertEquals("Current location is invalid", error.getMessage());
        verify(scooterMapper, never()).selectById(any());
    }

    @Test
    void planWalkingRouteToScooterRejectsEmptyPathResponse() throws Exception {
        Scooter scooter = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .status("AVAILABLE")
                .rentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .build();
        when(scooterMapper.selectById(1L)).thenReturn(scooter);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn("""
                {"status":"1","route":{"paths":[]}}
                """);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> routePlanningService.planWalkingRouteToScooter(
                        1L,
                        new BigDecimal("103.980000"),
                        new BigDecimal("30.767000")
                )
        );

        assertTrue(error.getMessage().contains("No walking route"));
    }

    @Test
    void planWalkingRouteToScooterRejectsStorePickupScooter() throws Exception {
        Scooter scooter = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .status("AVAILABLE")
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .build();
        when(scooterMapper.selectById(1L)).thenReturn(scooter);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> routePlanningService.planWalkingRouteToScooter(
                        1L,
                        new BigDecimal("103.980000"),
                        new BigDecimal("30.767000")
                )
        );

        assertEquals("Walking route is only available for scan ride scooters", error.getMessage());
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
