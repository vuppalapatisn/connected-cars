package com.toyota.connectedcars.telemetry.service;

import com.toyota.connectedcars.common.dto.TelemetryMessage;
import com.toyota.connectedcars.telemetry.config.KafkaConfig;
import com.toyota.connectedcars.telemetry.domain.TelemetryEvent;
import com.toyota.connectedcars.telemetry.domain.TelemetryEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryEventRepository repository;
    private final KafkaTemplate<String, TelemetryMessage> kafka;
    private final Counter ingested;

    public TelemetryService(TelemetryEventRepository repository,
                            KafkaTemplate<String, TelemetryMessage> kafka,
                            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafka = kafka;
        this.ingested = Counter.builder("telemetry.events.ingested")
                .description("Total telemetry events ingested")
                .register(meterRegistry);
    }

    public TelemetryEvent ingest(TelemetryMessage msg) {
        Instant recordedAt = msg.recordedAt() != null ? msg.recordedAt() : Instant.now();
        TelemetryEvent event = new TelemetryEvent(
                msg.vin(), msg.latitude(), msg.longitude(), msg.speedKph(),
                msg.batteryPercent(), msg.fuelPercent(), msg.diagnosticCode(), recordedAt);
        TelemetryEvent saved = repository.save(event);

        // Partition by VIN so all events for a vehicle stay ordered on one partition.
        kafka.send(KafkaConfig.TELEMETRY_TOPIC, msg.vin(), msg);
        ingested.increment();
        log.debug("Ingested telemetry for VIN {} at {}", msg.vin(), recordedAt);
        return saved;
    }

    public List<TelemetryEvent> latestForVehicle(String vin, int limit) {
        return repository.findByVinOrderByRecordedAtDesc(vin, PageRequest.of(0, limit));
    }
}
