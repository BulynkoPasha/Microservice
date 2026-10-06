package com.example.apigateway.client;

import com.example.apigateway.dto.UserCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    public Mono<Void> createUser(UserCreateRequestDto request, String accessToken) {
        return webClientBuilder.baseUrl(userServiceUrl).build()
                .post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}