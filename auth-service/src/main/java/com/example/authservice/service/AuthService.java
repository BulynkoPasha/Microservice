package com.example.authservice.service;

import com.example.authservice.dto.request.LoginRequest;
import com.example.authservice.dto.request.RefreshRequest;
import com.example.authservice.dto.request.RegisterRequest;
import com.example.authservice.dto.response.TokenResponse;
import com.example.authservice.dto.response.ValidateResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);

    ValidateResponse validate(String accessToken);
}