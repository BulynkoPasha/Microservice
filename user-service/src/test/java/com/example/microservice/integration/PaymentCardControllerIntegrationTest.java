package com.example.microservice.integration;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private Long createUser(String email, Long userId) {
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .name("Ivan").surname("Petrov").email(email)
                .birthDate(LocalDate.of(1990, 1, 1)).build();
        ResponseEntity<UserResponseDto> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                authHeader(request, userId, "USER"),
                UserResponseDto.class);
        return response.getBody().getId();
    }

    private PaymentCardCreateRequestDto cardRequest(Long userId) {
        return PaymentCardCreateRequestDto.builder()
                .userId(userId).number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(2)).build();
    }

    private ResponseEntity<PaymentCardResponseDto> createCard(Long userId) {
        return restTemplate.exchange(
                "/api/v1/cards",
                HttpMethod.POST,
                authHeader(cardRequest(userId), userId, "USER"),
                PaymentCardResponseDto.class);
    }

    @Test
    void createCard_shouldReturn201_whenUnderLimit() {
        Long userId = createUser("card1@test.com", 20L);

        ResponseEntity<PaymentCardResponseDto> response = createCard(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
    }

    @Test
    void createCard_shouldReturn400_whenLimitExceeded() {
        Long userId = createUser("card2@test.com", 21L);

        for (int i = 0; i < 5; i++) {
            createCard(userId);
        }

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/cards",
                HttpMethod.POST,
                authHeader(cardRequest(userId), userId, "USER"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCard_shouldReturn404_whenUserMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/cards",
                HttpMethod.POST,
                authHeader(cardRequest(999999L), 1L, "ADMIN"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCardsByUserId_shouldReturnAllCardsForUser() {
        Long userId = createUser("card3@test.com", 22L);
        createCard(userId);
        createCard(userId);

        ResponseEntity<PaymentCardResponseDto[]> response = restTemplate.exchange(
                "/api/v1/cards/user/" + userId,
                HttpMethod.GET,
                authHeader(userId, "USER"),
                PaymentCardResponseDto[].class);

        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void activateAndDeactivateCard_shouldToggleActiveStatus() {
        Long userId = createUser("card4@test.com", 23L);
        Long cardId = createCard(userId).getBody().getId();

        restTemplate.exchange("/api/v1/cards/" + cardId + "/deactivate",
                HttpMethod.PATCH, authHeader(userId, "USER"), PaymentCardResponseDto.class);

        ResponseEntity<PaymentCardResponseDto> afterDeactivate = restTemplate.exchange(
                "/api/v1/cards/" + cardId,
                HttpMethod.GET,
                authHeader(userId, "USER"),
                PaymentCardResponseDto.class);
        assertThat(afterDeactivate.getBody().isActive()).isFalse();
    }
}