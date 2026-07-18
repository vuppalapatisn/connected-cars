package com.toyota.connectedcars.telemetry.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "telemetry_event", indexes = {
        @Index(name = "idx_vehicle_ts", columnList = "vin, recordedAt")
})
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String vin;

    private double latitude;
    private double longitude;
    private double speedKph;
    private double batteryPercent;
    private double fuelPercent;
    private String diagnosticCode;

    @Column(nullable = false)
    private Instant recordedAt;

    protected TelemetryEvent() {
    }

    public TelemetryEvent(String vin, double latitude, double longitude, double speedKph,
                          double batteryPercent, double fuelPercent, String diagnosticCode, Instant recordedAt) {
        this.vin = vin;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKph = speedKph;
        this.batteryPercent = batteryPercent;
        this.fuelPercent = fuelPercent;
        this.diagnosticCode = diagnosticCode;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public String getVin() { return vin; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getSpeedKph() { return speedKph; }
    public double getBatteryPercent() { return batteryPercent; }
    public double getFuelPercent() { return fuelPercent; }
    public String getDiagnosticCode() { return diagnosticCode; }
    public Instant getRecordedAt() { return recordedAt; }
}
