package com.greengo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.MapRoutePoint;
import com.greengo.domain.Scooter;
import com.greengo.domain.ScooterWalkingRoute;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.RoutePlanningService;
import com.greengo.utils.RentalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AmapRoutePlanningService implements RoutePlanningService {

    private static final Logger log = LoggerFactory.getLogger(AmapRoutePlanningService.class);
    private static final String ROUTE_MODE_WALKING = "WALKING";
    private final ScooterMapper scooterMapper;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${amap.web-service-key:}")
    private String webServiceKey;

    @Autowired
    public AmapRoutePlanningService(ScooterMapper scooterMapper) {
        this(
                scooterMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper()
        );
    }

    public AmapRoutePlanningService(ScooterMapper scooterMapper, HttpClient httpClient, ObjectMapper objectMapper) {
        this.scooterMapper = scooterMapper;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScooterWalkingRoute planWalkingRouteToScooter(Long scooterId, BigDecimal fromLongitude, BigDecimal fromLatitude) {
        validateOrigin(fromLongitude, fromLatitude);
        Scooter scooter = validateScooter(scooterId);

        if (webServiceKey == null || webServiceKey.isBlank()) {
            throw new IllegalArgumentException("AMap route service is not configured");
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/direction/walking")
                    .queryParam("key", webServiceKey)
                    .queryParam("origin", buildLocation(fromLongitude, fromLatitude))
                    .queryParam("destination", buildLocation(scooter.getLongitude(), scooter.getLatitude()))
                    .build(true)
                    .toUri();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            return buildWalkingRoute(root, scooter, fromLongitude, fromLatitude);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AMap walking route request failed for scooter {}", scooterId, e);
            throw new IllegalArgumentException("Failed to plan walking route");
        }
    }

    ScooterWalkingRoute buildWalkingRoute(JsonNode root, Scooter scooter, BigDecimal fromLongitude, BigDecimal fromLatitude) {
        if (root == null || !"1".equals(root.path("status").asText())) {
            throw new IllegalArgumentException("Failed to plan walking route");
        }

        JsonNode firstPath = root.path("route").path("paths").path(0);
        if (firstPath.isMissingNode() || firstPath.isNull()) {
            throw new IllegalArgumentException("No walking route found");
        }

        List<MapRoutePoint> points = flattenRoutePoints(firstPath.path("steps"));
        if (points.isEmpty()) {
            throw new IllegalArgumentException("No walking route found");
        }
        points = normalizeRouteEndpoints(
                points,
                fromLongitude,
                fromLatitude,
                scooter.getLongitude(),
                scooter.getLatitude()
        );

        return ScooterWalkingRoute.builder()
                .scooterId(scooter.getId())
                .scooterCode(scooter.getScooterCode())
                .fromLongitude(fromLongitude)
                .fromLatitude(fromLatitude)
                .toLongitude(scooter.getLongitude())
                .toLatitude(scooter.getLatitude())
                .distanceMeters(parseLong(firstPath.path("distance").asText()))
                .durationSeconds(parseLong(firstPath.path("duration").asText()))
                .routeMode(ROUTE_MODE_WALKING)
                .points(points)
                .build();
    }

    private Scooter validateScooter(Long scooterId) {
        if (scooterId == null) {
            throw new IllegalArgumentException("Scooter id is required");
        }

        Scooter scooter = scooterMapper.selectById(scooterId);
        if (scooter == null) {
            throw new IllegalArgumentException("Scooter not found");
        }
        if (!hasValidCoordinates(scooter.getLongitude(), scooter.getLatitude())) {
            throw new IllegalArgumentException("Scooter coordinates are invalid");
        }
        if (!RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(scooter.getRentalMode())) {
            throw new IllegalArgumentException("Walking route is only available for scan ride scooters");
        }
        if (!RentalConstants.SCOOTER_STATUS_AVAILABLE.equalsIgnoreCase(scooter.getStatus())) {
            throw new IllegalArgumentException("Scooter is not available");
        }
        return scooter;
    }

    private void validateOrigin(BigDecimal longitude, BigDecimal latitude) {
        if (!hasValidCoordinates(longitude, latitude)) {
            throw new IllegalArgumentException("Current location is invalid");
        }
    }

    private boolean hasValidCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        return isBetween(longitude, new BigDecimal("-180"), new BigDecimal("180"))
                && isBetween(latitude, new BigDecimal("-90"), new BigDecimal("90"));
    }

    private boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private String buildLocation(BigDecimal longitude, BigDecimal latitude) {
        return longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
    }

    private Long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private List<MapRoutePoint> flattenRoutePoints(JsonNode stepsNode) {
        List<MapRoutePoint> points = new ArrayList<>();
        if (stepsNode == null || !stepsNode.isArray()) {
            return points;
        }

        for (JsonNode step : stepsNode) {
            String polyline = step.path("polyline").asText();
            if (polyline == null || polyline.isBlank()) {
                continue;
            }

            String[] segments = polyline.split(";");
            for (String segment : segments) {
                MapRoutePoint point = parsePoint(segment);
                if (point == null) {
                    continue;
                }
                if (!points.isEmpty() && isSamePoint(points.get(points.size() - 1), point)) {
                    continue;
                }
                points.add(point);
            }
        }
        return points;
    }

    private List<MapRoutePoint> normalizeRouteEndpoints(List<MapRoutePoint> points,
                                                        BigDecimal fromLongitude,
                                                        BigDecimal fromLatitude,
                                                        BigDecimal toLongitude,
                                                        BigDecimal toLatitude) {
        List<MapRoutePoint> normalized = new ArrayList<>(points);
        MapRoutePoint origin = buildPoint(fromLongitude, fromLatitude);
        MapRoutePoint destination = buildPoint(toLongitude, toLatitude);

        if (normalized.isEmpty() || !isSamePoint(normalized.get(0), origin)) {
            normalized.add(0, origin);
        }
        if (!isSamePoint(normalized.get(normalized.size() - 1), destination)) {
            normalized.add(destination);
        }
        return normalized;
    }

    private MapRoutePoint parsePoint(String segment) {
        if (segment == null || segment.isBlank()) {
            return null;
        }

        String[] pair = segment.split(",");
        if (pair.length != 2) {
            return null;
        }

        try {
            BigDecimal longitude = new BigDecimal(pair[0].trim());
            BigDecimal latitude = new BigDecimal(pair[1].trim());
            return buildPoint(longitude, latitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MapRoutePoint buildPoint(BigDecimal longitude, BigDecimal latitude) {
        return MapRoutePoint.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private boolean isSamePoint(MapRoutePoint left, MapRoutePoint right) {
        return Objects.equals(left.getLatitude(), right.getLatitude())
                && Objects.equals(left.getLongitude(), right.getLongitude());
    }
}
