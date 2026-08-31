package com.example.apigateway.client;

import com.example.apigateway.dto.RegistrationRequestDto;
import com.example.apigateway.dto.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    @Value("${internal.secret}")
    private String internalSecret;

    public Mono<TokenResponseDto> register(RegistrationRequestDto request) {
        return webClientBuilder.baseUrl(authServiceUrl).build()
                .post()
                .uri("/api/v1/auth/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TokenResponseDto.class);
    }

    public Mono<Void> rollbackRegistration(Long userId) {
        return webClientBuilder.baseUrl(authServiceUrl).build()
                .delete()
                .uri("/internal/credentials/{userId}", userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnSuccess(v -> log.info("Rollback succeeded for userId={}", userId))
                .onErrorResume(ex -> {
                    log.error("Rollback FAILED for userId={}. Manual cleanup required.", userId, ex);
                    return Mono.empty();
                });
    }

}
