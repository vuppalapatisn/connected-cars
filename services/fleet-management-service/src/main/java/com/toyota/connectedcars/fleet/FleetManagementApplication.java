package com.toyota.connectedcars.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.toyota.connectedcars.fleet", "com.toyota.connectedcars.common"})
public class FleetManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetManagementApplication.class, args);
    }
}
