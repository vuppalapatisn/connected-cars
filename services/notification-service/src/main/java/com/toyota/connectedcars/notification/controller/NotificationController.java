package com.toyota.connectedcars.notification.controller;

import com.toyota.connectedcars.notification.domain.Alert;
import com.toyota.connectedcars.notification.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final AlertService alertService;

    public NotificationController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public List<Alert> recent(@RequestParam(defaultValue = "50") int limit) {
        return alertService.recentAlerts(limit);
    }

    @GetMapping("/alerts/{vin}")
    public List<Alert> forVehicle(@PathVariable String vin,
                                  @RequestParam(defaultValue = "50") int limit) {
        return alertService.recentAlertsForVin(vin, limit);
    }
}
