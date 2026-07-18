package com.toyota.connectedcars.telemetry.api;

import com.toyota.connectedcars.common.dto.TelemetryMessage;
import com.toyota.connectedcars.telemetry.domain.TelemetryEvent;
import com.toyota.connectedcars.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService service;

    public TelemetryController(TelemetryService service) {
        this.service = service;
    }

    public record IngestRequest(
            @NotBlank String vin,
            double latitude,
            double longitude,
            double speedKph,
            double batteryPercent,
            double fuelPercent,
            String diagnosticCode) {}

    @PostMapping
    public ResponseEntity<TelemetryEvent> ingest(@Valid @RequestBody IngestRequest req) {
        TelemetryMessage msg = new TelemetryMessage(
                req.vin(), req.latitude(), req.longitude(), req.speedKph(),
                req.batteryPercent(), req.fuelPercent(), req.diagnosticCode(), null);
        return ResponseEntity.status(202).body(service.ingest(msg));
    }

    @GetMapping("/{vin}/latest")
    public List<TelemetryEvent> latest(@PathVariable String vin,
                                       @RequestParam(defaultValue = "20") int limit) {
        return service.latestForVehicle(vin, Math.min(limit, 200));
    }
}
