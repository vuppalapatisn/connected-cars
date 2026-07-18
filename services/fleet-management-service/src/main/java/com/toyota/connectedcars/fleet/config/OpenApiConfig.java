package com.toyota.connectedcars.fleet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fleetServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Fleet Management Service API")
                .description("Vehicle registration and inventory management for the Connected Cars fleet.")
                .version("1.0.0")
                .contact(new Contact().name("Connected Cars Platform"))
                .license(new License().name("MIT")));
    }
}
