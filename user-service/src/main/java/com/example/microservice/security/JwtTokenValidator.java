package com.example.microservice.security;

import com.example.microservice.exception.InvalidJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                throw new JwtException("Token is not an access token");
            }

            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            return new AuthenticatedUser(userId, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtException("Invalid or expired token");
        }
    }
}