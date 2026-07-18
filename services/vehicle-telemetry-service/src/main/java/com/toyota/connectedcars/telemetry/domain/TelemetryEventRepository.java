package com.toyota.connectedcars.telemetry.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, String> {
    List<TelemetryEvent> findByVinOrderByRecordedAtDesc(String vin, Pageable pageable);
}
