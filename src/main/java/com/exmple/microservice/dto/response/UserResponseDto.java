package com.exmple.microservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
