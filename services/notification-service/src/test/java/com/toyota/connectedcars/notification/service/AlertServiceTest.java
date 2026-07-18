package com.toyota.connectedcars.notification.service;

import com.toyota.connectedcars.common.dto.TelemetryMessage;
import com.toyota.connectedcars.notification.domain.Alert;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest {

    private final AlertService service = new AlertService();

    @Test
    void raisesLowBatteryWarning() {
        List<Alert> alerts = service.evaluate(msg(50, 10, 0, null));
        assertThat(alerts).extracting(Alert::type).contains("LOW_BATTERY");
    }

    @Test
    void raisesOverspeedCritical() {
        List<Alert> alerts = service.evaluate(msg(180, 80, 80, null));
        assertThat(alerts).anyMatch(a -> a.type().equals("OVERSPEED") && a.severity().equals("CRITICAL"));
    }

    @Test
    void raisesDtcAlert() {
        List<Alert> alerts = service.evaluate(msg(60, 80, 80, "P0301"));
        assertThat(alerts).extracting(Alert::type).contains("DTC");
    }

    @Test
    void noAlertsForHealthyTelemetry() {
        List<Alert> alerts = service.evaluate(msg(60, 80, 80, ""));
        assertThat(alerts).isEmpty();
    }

    private TelemetryMessage msg(double speed, double battery, double fuel, String dtc) {
        return new TelemetryMessage("JT123", 35.0, 139.0, speed, battery, fuel, dtc, Instant.now());
    }
}
