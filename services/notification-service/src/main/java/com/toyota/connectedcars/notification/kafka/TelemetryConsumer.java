package com.toyota.connectedcars.notification.kafka;

import com.toyota.connectedcars.common.dto.TelemetryMessage;
import com.toyota.connectedcars.notification.service.AlertService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryConsumer {

    private final AlertService alertService;

    public TelemetryConsumer(AlertService alertService) {
        this.alertService = alertService;
    }

    @KafkaListener(topics = "vehicle.telemetry", groupId = "notification-service")
    public void onTelemetry(TelemetryMessage message) {
        alertService.evaluate(message);
    }
}
