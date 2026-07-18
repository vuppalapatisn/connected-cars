package com.toyota.connectedcars.notification.service;

import com.toyota.connectedcars.common.dto.TelemetryMessage;
import com.toyota.connectedcars.notification.domain.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Evaluates telemetry against alert rules and dispatches notifications.
 * In production, dispatch would call SNS / SES / Pinpoint. Here we keep an
 * in-memory ring buffer so alerts are queryable via REST.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final int MAX_RETAINED = 500;

    private final ConcurrentLinkedDeque<Alert> recent = new ConcurrentLinkedDeque<>();

    public List<Alert> evaluate(TelemetryMessage t) {
        List<Alert> alerts = new ArrayList<>();

        if (t.batteryPercent() > 0 && t.batteryPercent() < 15) {
            alerts.add(Alert.of(t.vin(), "LOW_BATTERY", "WARNING",
                    "Battery at %.0f%%".formatted(t.batteryPercent())));
        }
        if (t.fuelPercent() > 0 && t.fuelPercent() < 10) {
            alerts.add(Alert.of(t.vin(), "LOW_FUEL", "WARNING",
                    "Fuel at %.0f%%".formatted(t.fuelPercent())));
        }
        if (t.speedKph() > 160) {
            alerts.add(Alert.of(t.vin(), "OVERSPEED", "CRITICAL",
                    "Speed %.0f kph exceeds threshold".formatted(t.speedKph())));
        }
        if (t.diagnosticCode() != null && !t.diagnosticCode().isBlank()) {
            alerts.add(Alert.of(t.vin(), "DTC", "CRITICAL",
                    "Diagnostic trouble code: " + t.diagnosticCode()));
        }

        alerts.forEach(this::dispatch);
        return alerts;
    }

    private void dispatch(Alert alert) {
        // TODO: integrate AWS SNS / Pinpoint. For now log + retain.
        log.info("ALERT [{}] {} for VIN {}: {}", alert.severity(), alert.type(), alert.vin(), alert.message());
        recent.addFirst(alert);
        while (recent.size() > MAX_RETAINED) {
            recent.pollLast();
        }
    }

    public List<Alert> recentAlerts(int limit) {
        List<Alert> snapshot = new ArrayList<>(recent);
        return snapshot.subList(0, Math.min(limit, snapshot.size()));
    }

    public List<Alert> recentAlertsForVin(String vin, int limit) {
        return Collections.unmodifiableList(
                recent.stream().filter(a -> a.vin().equals(vin)).limit(limit).toList());
    }
}
