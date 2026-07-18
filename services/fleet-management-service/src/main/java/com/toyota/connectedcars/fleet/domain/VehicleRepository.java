package com.toyota.connectedcars.fleet.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByOwner(String owner);
    List<Vehicle> findByStatus(String status);
}
