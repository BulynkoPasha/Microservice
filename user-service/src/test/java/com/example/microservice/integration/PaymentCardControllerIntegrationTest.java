package com.example.microservice.integration;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private Long createUser(String email) {
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .name("Ivan").surname("Petrov").email(email)
                .birthDate(LocalDate.of(1990, 1, 1)).build();
        return restTemplate.postForEntity("/api/v1/users", request, UserResponseDto.class)
                .getBody().getId();
    }

    private PaymentCardCreateRequestDto cardRequest(Long userId) {
        return PaymentCardCreateRequestDto.builder()
                .userId(userId).number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(2)).build();
    }

    @Test
    void createCard_shouldReturn201_whenUnderLimit() {
        Long userId = createUser("card1@test.com");

        ResponseEntity<PaymentCardResponseDto> response = restTemplate.postForEntity(
                "/api/v1/cards", cardRequest(userId), PaymentCardResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
    }

    @Test
    void createCard_shouldReturn400_whenLimitExceeded() {
        Long userId = createUser("card2@test.com");

        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/api/v1/cards", cardRequest(userId), PaymentCardResponseDto.class);
        }

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/cards", cardRequest(userId), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCard_shouldReturn404_whenUserMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/cards", cardRequest(999999L), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCardsByUserId_shouldReturnAllCardsForUser() {
        Long userId = createUser("card3@test.com");
        restTemplate.postForEntity("/api/v1/cards", cardRequest(userId), PaymentCardResponseDto.class);
        restTemplate.postForEntity("/api/v1/cards", cardRequest(userId), PaymentCardResponseDto.class);

        ResponseEntity<PaymentCardResponseDto[]> response = restTemplate.getForEntity(
                "/api/v1/cards/user/" + userId, PaymentCardResponseDto[].class);

        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void activateAndDeactivateCard_shouldToggleActiveStatus() {
        Long userId = createUser("card4@test.com");
        Long cardId = restTemplate.postForEntity(
                "/api/v1/cards", cardRequest(userId), PaymentCardResponseDto.class).getBody().getId();

        restTemplate.exchange("/api/v1/cards/" + cardId + "/deactivate",
                org.springframework.http.HttpMethod.PATCH, null, PaymentCardResponseDto.class);

        ResponseEntity<PaymentCardResponseDto> afterDeactivate = restTemplate.getForEntity(
                "/api/v1/cards/" + cardId, PaymentCardResponseDto.class);
        assertThat(afterDeactivate.getBody().isActive()).isFalse();
    }
}