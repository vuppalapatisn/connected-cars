package com.toyota.connectedcars.auth.config;

import com.toyota.connectedcars.auth.domain.AppUser;
import com.toyota.connectedcars.auth.domain.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    /**
     * The auth-service endpoints are public (login/register/introspect); real
     * network-level protection is provided by the Istio mesh (mTLS + AuthorizationPolicy).
     */
    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
            }
        };
    }

    /** Seed a couple of demo accounts for local dev. */
    @Bean
    @Profile({"dev", "default"})
    public CommandLineRunner seed(AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByUsername("driver1")) {
                users.save(new AppUser("driver1", encoder.encode("password"), Set.of("ROLE_DRIVER")));
            }
            if (!users.existsByUsername("fleetadmin")) {
                users.save(new AppUser("fleetadmin", encoder.encode("password"), Set.of("ROLE_FLEET_ADMIN", "ROLE_DRIVER")));
            }
        };
    }
}
