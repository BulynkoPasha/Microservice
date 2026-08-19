package com.example.apigateway.controller;

import com.example.apigateway.client.AuthServiceClient;
import com.example.apigateway.client.UserServiceClient;
import com.example.apigateway.dto.RegistrationRequestDto;
import com.example.apigateway.dto.TokenResponseDto;
import com.example.apigateway.dto.UserCreateRequestDto;
import com.example.apigateway.exception.RegistrationFailedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final AuthServiceClient authServiceClient;
    private final UserServiceClient userServiceClient;

    @PostMapping("/register")
    public Mono<ResponseEntity<TokenResponseDto>> register(@Valid @RequestBody RegistrationRequestDto request) {
        return authServiceClient.register(request)
                .flatMap(tokenResponse -> createUserProfile(request, tokenResponse));
    }

    private Mono<ResponseEntity<TokenResponseDto>> createUserProfile(
            RegistrationRequestDto request, TokenResponseDto tokenResponse) {

        UserCreateRequestDto userRequest = UserCreateRequestDto.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .email(request.getLogin())
                .birthDate(request.getBirthDate())
                .build();

        return userServiceClient.createUser(userRequest, tokenResponse.getAccessToken())
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse)))
                .onErrorResume(ex -> rollbackAndFail(tokenResponse.getUserId(), ex));
    }

    private Mono<ResponseEntity<TokenResponseDto>> rollbackAndFail(Long userId, Throwable ex) {
        log.warn("User profile creation failed, rolling back credentials for userId={}", userId, ex);

        return authServiceClient.rollbackRegistration(userId)
                .then(Mono.error(new RegistrationFailedException(
                        "Registration could not be completed. Please try again.")));
    }
}