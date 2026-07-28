package com.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String ISSUER = "auth-service";
    private static final String AUDIENCE = "microservice-platform";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role) {
        return generateToken(userId, role, TokenType.ACCESS,
                jwtProperties.getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(Long userId, String role) {
        return generateToken(userId, role, TokenType.REFRESH,
                jwtProperties.getRefreshTokenExpirationMs());
    }

    private String generateToken(Long userId, String role, TokenType type, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", type.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .requireIssuer(ISSUER)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        try {
            return Long.valueOf(parseClaims(token).getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalStateException("Malformed token claims");
        }
    }

    public String getRole(String token) {
        try {
            String role = parseClaims(token).get("role", String.class);
            if (role == null) {
                throw new IllegalStateException("Malformed token claims");
            }
            return role;
        } catch (JwtException ex) {
            throw new IllegalStateException("Malformed token claims");
        }
    }

    public TokenType getTokenType(String token) {
        try {
            String type = parseClaims(token).get("type", String.class);
            return TokenType.valueOf(type);
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new IllegalStateException("Malformed token claims");
        }
    }

    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }
}