package com.toyota.connectedcars.fleet.controller;

import com.toyota.connectedcars.fleet.domain.Vehicle;
import com.toyota.connectedcars.fleet.domain.VehicleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/fleet/vehicles")
public class FleetController {

    private final VehicleRepository repository;

    public FleetController(VehicleRepository repository) {
        this.repository = repository;
    }

    public record RegisterVehicleRequest(
            @NotBlank String vin,
            @NotBlank String model,
            int modelYear,
            @NotBlank String owner) {}

    @PostMapping
    public ResponseEntity<Vehicle> register(@Valid @RequestBody RegisterVehicleRequest req) {
        Vehicle v = new Vehicle(req.vin(), req.model(), req.modelYear(), req.owner());
        return ResponseEntity.status(201).body(repository.save(v));
    }

    @GetMapping
    public List<Vehicle> list(@RequestParam(required = false) String owner,
                              @RequestParam(required = false) String status) {
        if (owner != null) return repository.findByOwner(owner);
        if (status != null) return repository.findByStatus(status);
        return repository.findAll();
    }

    @GetMapping("/{vin}")
    public Vehicle get(@PathVariable String vin) {
        return repository.findById(vin)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vehicle not found"));
    }

    @PatchMapping("/{vin}/status")
    public Vehicle updateStatus(@PathVariable String vin, @RequestParam String status) {
        Vehicle v = get(vin);
        v.setStatus(status);
        return repository.save(v);
    }

    @DeleteMapping("/{vin}")
    public ResponseEntity<Void> delete(@PathVariable String vin) {
        repository.deleteById(vin);
        return ResponseEntity.noContent().build();
    }
}
