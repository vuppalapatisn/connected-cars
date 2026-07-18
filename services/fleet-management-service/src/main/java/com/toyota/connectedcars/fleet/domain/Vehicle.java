package com.toyota.connectedcars.fleet.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    private String vin;

    @Column(nullable = false)
    private String model;

    private int modelYear;
    private String owner;
    private String status; // ACTIVE, MAINTENANCE, DECOMMISSIONED

    @Column(nullable = false)
    private Instant registeredAt;

    protected Vehicle() {
    }

    public Vehicle(String vin, String model, int modelYear, String owner) {
        this.vin = vin;
        this.model = model;
        this.modelYear = modelYear;
        this.owner = owner;
        this.status = "ACTIVE";
        this.registeredAt = Instant.now();
    }

    public String getVin() { return vin; }
    public String getModel() { return model; }
    public int getModelYear() { return modelYear; }
    public String getOwner() { return owner; }
    public String getStatus() { return status; }
    public Instant getRegisteredAt() { return registeredAt; }

    public void setModel(String model) { this.model = model; }
    public void setModelYear(int modelYear) { this.modelYear = modelYear; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setStatus(String status) { this.status = status; }
}
