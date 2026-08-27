package com.example.authservice.service.impl;

import com.example.authservice.dto.request.LoginRequest;
import com.example.authservice.dto.request.RefreshRequest;
import com.example.authservice.dto.request.RegisterRequest;
import com.example.authservice.dto.response.TokenResponse;
import com.example.authservice.dto.response.ValidateResponse;
import com.example.authservice.entity.Credential;
import com.example.authservice.entity.RefreshToken;
import com.example.authservice.entity.Role;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.repository.CredentialRepository;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.security.TokenType;
import com.example.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (credentialRepository.existsByLogin(request.getLogin())) {
            throw new InvalidCredentialsException("Registration failed");
        }

        Credential credential = Credential.builder()
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .build();

        Credential saved = credentialRepository.save(credential);
        saved.setUserId(saved.getId());

        return issueTokenPair(saved.getUserId(), saved.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid login or password");
        }

        if (!credential.isActive()) {
            throw new InvalidCredentialsException("Invalid login or password");
        }

        return issueTokenPair(credential.getUserId(), credential.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.isValid(request.getRefreshToken())
                || jwtTokenProvider.getTokenType(request.getRefreshToken()) != TokenType.REFRESH) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String incomingHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        Credential credential = credentialRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        return issueTokenPair(credential.getUserId(), credential.getRole().name());
    }

    @Override
    public ValidateResponse validate(String accessToken) {
        if (!jwtTokenProvider.isValid(accessToken)
                || jwtTokenProvider.getTokenType(accessToken) != TokenType.ACCESS) {
            return ValidateResponse.builder().valid(false).build();
        }

        return ValidateResponse.builder()
                .valid(true)
                .userId(jwtTokenProvider.getUserId(accessToken))
                .role(jwtTokenProvider.getRole(accessToken))
                .build();
    }

    private TokenResponse issueTokenPair(Long userId, String role) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, role);

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(tokenEntity);

        return TokenResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("Internal server error");
        }
    }
}