package com.aivideo.auth;

import com.aivideo.common.exception.BadRequestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final AuthProperties properties;

    public JwtService(AuthProperties properties) {
        if (properties.jwtSecret() == null
                || properties.jwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BadRequestException("AUTH_CONFIG_ERROR",
                    "JWT secret must be at least 32 characters (env JWT_SECRET)");
        }
        this.properties = properties;
    }

    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.jwtExpiryHours() * 3600)))
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new BadRequestException("INVALID_TOKEN", "Invalid or expired token");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
