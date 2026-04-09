package com.greengo.service.impl;

import com.greengo.service.GeoAddressService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AmapGeoAddressService implements GeoAddressService {

    private static final Logger log = LoggerFactory.getLogger(AmapGeoAddressService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> addressCache = new ConcurrentHashMap<>();

    @Value("${amap.web-service-key:}")
    private String webServiceKey;

    @Override
    public String reverseGeocode(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null || webServiceKey == null || webServiceKey.isBlank()) {
            return null;
        }

        String cacheKey = longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
        String cachedAddress = addressCache.get(cacheKey);
        if (cachedAddress != null && !cachedAddress.isBlank()) {
            return cachedAddress;
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                    .queryParam("key", webServiceKey)
                    .queryParam("location", cacheKey)
                    .queryParam("radius", 1000)
                    .queryParam("extensions", "base")
                    .build(true)
                    .toUri();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                log.warn("AMap reverse geocode failed for {}: {}", cacheKey, root.path("info").asText());
                return null;
            }

            String address = root.path("regeocode").path("formatted_address").asText();
            if (address == null || address.isBlank()) {
                return null;
            }

            addressCache.put(cacheKey, address);
            return address;
        } catch (Exception e) {
            log.warn("AMap reverse geocode request failed for {}", cacheKey, e);
            return null;
        }
    }
}

