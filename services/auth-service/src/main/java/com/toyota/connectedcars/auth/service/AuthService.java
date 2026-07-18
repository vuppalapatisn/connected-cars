package com.toyota.connectedcars.auth.service;

import com.toyota.connectedcars.auth.controller.AuthController.IntrospectResponse;
import com.toyota.connectedcars.auth.controller.AuthController.TokenResponse;
import com.toyota.connectedcars.auth.domain.AppUser;
import com.toyota.connectedcars.auth.domain.AppUserRepository;
import com.toyota.connectedcars.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long expirationMs;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtService jwt,
                       @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.expirationMs = expirationMs;
    }

    public void register(String username, String password, Set<String> roles) {
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(CONFLICT, "Username already exists");
        }
        users.save(new AppUser(username, encoder.encode(password), roles));
    }

    public TokenResponse login(String username, String password) {
        AppUser user = users.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        String token = jwt.generateToken(
                user.getUsername(),
                List.copyOf(user.getRoles()),
                Map.of("uid", user.getId()));
        return new TokenResponse(token, "Bearer", expirationMs / 1000);
    }

    public IntrospectResponse introspect(String token) {
        if (!jwt.isValid(token)) {
            return new IntrospectResponse(false, null, List.of());
        }
        return new IntrospectResponse(true, jwt.extractSubject(token), jwt.extractRoles(token));
    }
}
