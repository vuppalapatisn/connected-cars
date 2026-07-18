package com.toyota.connectedcars.common.dto;

import java.time.Instant;

/**
 * Canonical telemetry event exchanged over Kafka between the telemetry producer
 * and downstream consumers (notification, analytics). Kept in common-lib so both
 * sides share one schema.
 */
public record TelemetryMessage(
        String vin,
        double latitude,
        double longitude,
        double speedKph,
        double batteryPercent,
        double fuelPercent,
        String diagnosticCode,
        Instant recordedAt) {
}
