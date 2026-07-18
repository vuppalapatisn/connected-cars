package com.toyota.connectedcars.notification.domain;

import java.time.Instant;

public record Alert(
        String vin,
        String type,
        String severity,
        String message,
        Instant createdAt) {

    public static Alert of(String vin, String type, String severity, String message) {
        return new Alert(vin, type, severity, message, Instant.now());
    }
}
