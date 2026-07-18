package com.toyota.connectedcars.auth.controller;

import com.toyota.connectedcars.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password, Set<String> roles) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
    public record IntrospectResponse(boolean active, String subject, List<String> roles) {}

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.username(), req.password(),
                req.roles() == null || req.roles().isEmpty() ? Set.of("ROLE_DRIVER") : req.roles());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.username(), req.password());
    }

    /** OAuth2-style token introspection used by the gateway / other services. */
    @PostMapping("/introspect")
    public IntrospectResponse introspect(@RequestParam("token") String token) {
        return authService.introspect(token);
    }
}
