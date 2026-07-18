package com.toyota.connectedcars.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Symmetric-key (HS256) JWT issue/validate helper shared by all services.
 *
 * <p>In production the {@code JWT_SECRET} is delivered via AWS Secrets Manager /
 * External Secrets Operator, and you would typically move to asymmetric (RS256)
 * signing with the auth-service holding the private key and everyone else the JWKS.</p>
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret:local-dev-secret-change-me-please-32bytes-min}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs,
            @Value("${jwt.issuer:https://auth.connected-cars.toyota.com}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(String subject, List<String> roles, Map<String, Object> extra) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs));
        if (extra != null) {
            extra.forEach(builder::claim);
        }
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List<?> l ? (List<String>) l : List.of();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
