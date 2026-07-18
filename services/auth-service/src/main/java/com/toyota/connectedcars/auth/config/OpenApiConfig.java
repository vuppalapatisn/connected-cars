package com.toyota.connectedcars.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Auth Service API")
                .description("Issues and introspects JWTs; manages user and device identity for the Connected Cars platform.")
                .version("1.0.0")
                .contact(new Contact().name("Connected Cars Platform"))
                .license(new License().name("MIT")));
    }
}
