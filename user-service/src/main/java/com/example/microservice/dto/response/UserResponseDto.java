package com.example.microservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class UserResponseDto {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
