package com.example.orderservice.client;

import com.example.orderservice.dto.response.UserInfoDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public UserServiceClient(@Value("${user-service.url}") String userServiceUrl,
                             CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
    }

    public UserInfoDto getUserInfo(Long userId, String authorizationHeader) {
        return circuitBreaker.executeSupplier(() -> fetchUser(userId, authorizationHeader));
    }

    private UserInfoDto fetchUser(Long userId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/{id}", userId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(UserInfoDto.class);
        } catch (RestClientException ex) {
            log.warn("Failed to fetch user info for userId={}: {}", userId, ex.getMessage());
            throw ex;
        }
    }
}