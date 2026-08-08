package com.example.microservice.integration;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.entity.User;
import com.example.microservice.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private UserCreateRequestDto validRequest(String email) {
        return UserCreateRequestDto.builder()
                .name("Ivan")
                .surname("Petrov")
                .email(email)
                .birthDate(LocalDate.of(1999, 1, 1))
                .build();
    }

    private ResponseEntity<UserResponseDto> createUser(String email, Long userId) {
        return restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                authHeader(validRequest(email), userId, "USER"),
                UserResponseDto.class);
    }

    private ResponseEntity<UserResponseDto> getUser(Long id, Long callerUserId, String role) {
        return restTemplate.exchange(
                "/api/v1/users/" + id,
                HttpMethod.GET,
                authHeader(callerUserId, role),
                UserResponseDto.class);
    }

    @Test
    void createUser_shouldReturn201_whenRequestValid() {
        ResponseEntity<UserResponseDto> response = createUser("ivan1@test.com", 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("ivan1@test.com");
        assertThat(response.getBody().isActive()).isTrue();
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() {
        createUser("dup@test.com", 10L);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                authHeader(validRequest("dup@test.com"), 11L, "USER"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createUser_shouldReturn400_whenNameBlank() {
        UserCreateRequestDto invalid = UserCreateRequestDto.builder()
                .name("").surname("Petrov").email("invalid@test.com")
                .birthDate(LocalDate.of(1999, 1, 1)).build();

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                authHeader(invalid, 12L, "USER"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUserById_shouldReturn200_whenUserExists() {
        ResponseEntity<UserResponseDto> created = createUser("get1@test.com", 2L);
        Long id = created.getBody().getId();

        ResponseEntity<UserResponseDto> response = getUser(id, id, "USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    void getUserById_shouldReturn404_whenUserMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/users/999999",
                HttpMethod.GET,
                authHeader(999999L, "ADMIN"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUser_shouldReturn200AndUpdatedData() {
        ResponseEntity<UserResponseDto> created = createUser("update1@test.com", 3L);
        Long id = created.getBody().getId();

        UserUpdateRequestDto update = UserUpdateRequestDto.builder()
                .name("Petr").surname("Sidorov").email("update1@test.com")
                .birthDate(LocalDate.of(2000, 2, 2)).build();

        restTemplate.exchange(
                "/api/v1/users/" + id,
                HttpMethod.PUT,
                authHeader(update, id, "USER"),
                UserResponseDto.class);

        ResponseEntity<UserResponseDto> response = getUser(id, id, "USER");

        assertThat(response.getBody().getName()).isEqualTo("Petr");
    }

    @Test
    void activateAndDeactivateUser_shouldToggleActiveStatus() {
        ResponseEntity<UserResponseDto> created = createUser("toggle1@test.com", 4L);
        Long id = created.getBody().getId();

        restTemplate.exchange("/api/v1/users/" + id + "/deactivate",
                HttpMethod.PATCH, authHeader(id, "ADMIN"), UserResponseDto.class);

        ResponseEntity<UserResponseDto> afterDeactivate = getUser(id, id, "ADMIN");
        assertThat(afterDeactivate.getBody().isActive()).isFalse();

        restTemplate.exchange("/api/v1/users/" + id + "/activate",
                HttpMethod.PATCH, authHeader(id, "ADMIN"), UserResponseDto.class);

        ResponseEntity<UserResponseDto> afterActivate = getUser(id, id, "ADMIN");
        assertThat(afterActivate.getBody().isActive()).isTrue();
    }

    @Test
    void getUserWithCards_shouldServeCachedValue_thenReflectUpdateAfterEviction() {
        ResponseEntity<UserResponseDto> created = createUser("cache2@test.com", 5L);
        Long id = created.getBody().getId();

        ResponseEntity<Map> firstCall = restTemplate.exchange(
                "/api/v1/users/" + id + "/with-cards",
                HttpMethod.GET,
                authHeader(id, "USER"),
                Map.class);
        assertThat(firstCall.getBody().get("name")).isEqualTo("Ivan");

        User user = userRepository.findById(id).orElseThrow();
        user.setName("ChangedDirectly");
        userRepository.save(user);

        ResponseEntity<Map> cachedCall = restTemplate.exchange(
                "/api/v1/users/" + id + "/with-cards",
                HttpMethod.GET,
                authHeader(id, "USER"),
                Map.class);
        assertThat(cachedCall.getBody().get("name")).isEqualTo("Ivan");

        UserUpdateRequestDto update = UserUpdateRequestDto.builder()
                .name("ChangedViaApi").surname("Petrov").email("cache2@test.com")
                .birthDate(LocalDate.of(1990, 1, 1)).build();
        restTemplate.exchange(
                "/api/v1/users/" + id,
                HttpMethod.PUT,
                authHeader(update, id, "USER"),
                UserResponseDto.class);

        ResponseEntity<Map> afterEviction = restTemplate.exchange(
                "/api/v1/users/" + id + "/with-cards",
                HttpMethod.GET,
                authHeader(id, "USER"),
                Map.class);
        assertThat(afterEviction.getBody().get("name")).isEqualTo("ChangedViaApi");
    }
}