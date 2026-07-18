package com.toyota.connectedcars.telemetry.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI telemetryServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Vehicle Telemetry Service API")
                .description("Ingests vehicle telemetry, persists it, and publishes to Kafka for downstream processing.")
                .version("1.0.0")
                .contact(new Contact().name("Connected Cars Platform"))
                .license(new License().name("MIT")));
    }
}
