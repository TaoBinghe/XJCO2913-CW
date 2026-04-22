package com.greengo.utils;

import com.greengo.domain.Scooter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ScooterDisplayMetrics {

    private static final Metrics DEFAULT_METRICS = new Metrics(80, new BigDecimal("25.0"));

    private static final Map<String, Metrics> METRICS_BY_CODE = Map.ofEntries(
            Map.entry("SC001", new Metrics(92, new BigDecimal("32.0"))),
            Map.entry("SC002", new Metrics(89, new BigDecimal("30.5"))),
            Map.entry("SC003", new Metrics(86, new BigDecimal("29.0"))),
            Map.entry("SC004", new Metrics(83, new BigDecimal("27.5"))),
            Map.entry("SC005", new Metrics(95, new BigDecimal("33.5"))),
            Map.entry("SC006", new Metrics(88, new BigDecimal("31.0"))),
            Map.entry("SC007", new Metrics(84, new BigDecimal("28.5"))),
            Map.entry("SC008", new Metrics(91, new BigDecimal("32.5"))),
            Map.entry("SC009", new Metrics(87, new BigDecimal("30.0"))),
            Map.entry("SC010", new Metrics(82, new BigDecimal("27.0"))),
            Map.entry("SC201", new Metrics(78, new BigDecimal("25.0"))),
            Map.entry("SC202", new Metrics(74, new BigDecimal("23.5"))),
            Map.entry("SC203", new Metrics(81, new BigDecimal("26.0"))),
            Map.entry("SC204", new Metrics(69, new BigDecimal("22.0"))),
            Map.entry("SC205", new Metrics(76, new BigDecimal("24.0")))
    );

    private ScooterDisplayMetrics() {
    }

    public static void enrich(Scooter scooter) {
        if (scooter == null) {
            return;
        }

        String scooterCode = scooter.getScooterCode();
        Metrics metrics = scooterCode == null
                ? DEFAULT_METRICS
                : METRICS_BY_CODE.getOrDefault(scooterCode, DEFAULT_METRICS);
        scooter.setBatteryLevel(metrics.batteryLevel());
        scooter.setRemainingRangeKm(metrics.remainingRangeKm());
    }

    public static void enrich(List<Scooter> scooters) {
        if (scooters == null || scooters.isEmpty()) {
            return;
        }
        scooters.forEach(ScooterDisplayMetrics::enrich);
    }

    private record Metrics(Integer batteryLevel, BigDecimal remainingRangeKm) {
    }
}
