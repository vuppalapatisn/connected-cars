package com.toyota.connectedcars.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Notification Service API")
                .description("Consumes telemetry events, evaluates alert rules, and exposes recent alerts.")
                .version("1.0.0")
                .contact(new Contact().name("Connected Cars Platform"))
                .license(new License().name("MIT")));
    }
}
