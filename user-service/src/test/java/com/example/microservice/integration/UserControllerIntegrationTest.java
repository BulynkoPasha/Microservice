package com.example.microservice.integration;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.entity.User;
import com.example.microservice.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
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

    @Test
    void createUser_shouldReturn201_whenRequestValid() {
        ResponseEntity<UserResponseDto> response = restTemplate.postForEntity(
                "/api/v1/users", validRequest("ivan1@test.com"), UserResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("ivan1@test.com");
        assertThat(response.getBody().isActive()).isTrue();
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() {
        restTemplate.postForEntity("/api/v1/users", validRequest("dup@test.com"), UserResponseDto.class);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/users", validRequest("dup@test.com"), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createUser_shouldReturn400_whenNameBlank() {
        UserCreateRequestDto invalid = UserCreateRequestDto.builder()
                .name("").surname("Petrov").email("invalid@test.com")
                .birthDate(LocalDate.of(1999, 1, 1)).build();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/users", invalid, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUserById_shouldReturn200_whenUserExists() {
        ResponseEntity<UserResponseDto> created = restTemplate.postForEntity(
                "/api/v1/users", validRequest("get1@test.com"), UserResponseDto.class);
        Long id = created.getBody().getId();

        ResponseEntity<UserResponseDto> response = restTemplate.getForEntity(
                "/api/v1/users/" + id, UserResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    void getUserById_shouldReturn404_whenUserMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/users/999999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUser_shouldReturn200AndUpdatedData() {
        ResponseEntity<UserResponseDto> created = restTemplate.postForEntity(
                "/api/v1/users", validRequest("update1@test.com"), UserResponseDto.class);
        Long id = created.getBody().getId();

        UserUpdateRequestDto update = UserUpdateRequestDto.builder()
                .name("Petr").surname("Sidorov").email("update1@test.com")
                .birthDate(LocalDate.of(2000, 2, 2)).build();

        restTemplate.put("/api/v1/users/" + id, update);

        ResponseEntity<UserResponseDto> response = restTemplate.getForEntity(
                "/api/v1/users/" + id, UserResponseDto.class);

        assertThat(response.getBody().getName()).isEqualTo("Petr");
    }

    @Test
    void activateAndDeactivateUser_shouldToggleActiveStatus() {
        ResponseEntity<UserResponseDto> created = restTemplate.postForEntity(
                "/api/v1/users", validRequest("toggle1@test.com"), UserResponseDto.class);
        Long id = created.getBody().getId();

        restTemplate.exchange("/api/v1/users/" + id + "/deactivate",
                org.springframework.http.HttpMethod.PATCH, null, UserResponseDto.class);

        ResponseEntity<UserResponseDto> afterDeactivate = restTemplate.getForEntity(
                "/api/v1/users/" + id, UserResponseDto.class);
        assertThat(afterDeactivate.getBody().isActive()).isFalse();

        restTemplate.exchange("/api/v1/users/" + id + "/activate",
                org.springframework.http.HttpMethod.PATCH, null, UserResponseDto.class);

        ResponseEntity<UserResponseDto> afterActivate = restTemplate.getForEntity(
                "/api/v1/users/" + id, UserResponseDto.class);
        assertThat(afterActivate.getBody().isActive()).isTrue();
    }

    @Test
    void getUserWithCards_shouldServeCachedValue_thenReflectUpdateAfterEviction() {
        ResponseEntity<UserResponseDto> created = restTemplate.postForEntity(
                "/api/v1/users", validRequest("cache2@test.com"), UserResponseDto.class);
        Long id = created.getBody().getId();

        ResponseEntity<Map> firstCall = restTemplate.getForEntity(
                "/api/v1/users/" + id + "/with-cards", Map.class);
        assertThat(firstCall.getBody().get("name")).isEqualTo("Ivan");

        User user = userRepository.findById(id).orElseThrow();
        user.setName("ChangedDirectly");
        userRepository.save(user);

        ResponseEntity<Map> cachedCall = restTemplate.getForEntity(
                "/api/v1/users/" + id + "/with-cards", Map.class);

        assertThat(cachedCall.getBody().get("name")).isEqualTo("Ivan");

        UserUpdateRequestDto update = UserUpdateRequestDto.builder()
                .name("ChangedViaApi").surname("Petrov").email("cache2@test.com")
                .birthDate(LocalDate.of(1990, 1, 1)).build();
        restTemplate.put("/api/v1/users/" + id, update);

        ResponseEntity<Map> afterEviction = restTemplate.getForEntity(
                "/api/v1/users/" + id + "/with-cards", Map.class);
        assertThat(afterEviction.getBody().get("name")).isEqualTo("ChangedViaApi");
    }

    @Test
    void getUserWithCards_shouldServeCachedResultOnSecondCall_reflectingUpdate() {
        ResponseEntity<UserResponseDto> created = restTemplate.postForEntity(
                "/api/v1/users", validRequest("cache1@test.com"), UserResponseDto.class);
        Long id = created.getBody().getId();

        // first call
        restTemplate.getForEntity("/api/v1/users/" + id + "/with-cards", Object.class);

        // chanhe user
        UserUpdateRequestDto update = UserUpdateRequestDto.builder()
                .name("Changed").surname("Petrov").email("cache1@test.com")
                .birthDate(LocalDate.of(1999, 1, 1)).build();
        restTemplate.put("/api/v1/users/" + id, update);

        // second call (wixth cash)
        ResponseEntity<java.util.Map> response = restTemplate.getForEntity(
                "/api/v1/users/" + id + "/with-cards", Map.class);

        assertThat(response.getBody().get("name")).isEqualTo("Changed");
    }
}