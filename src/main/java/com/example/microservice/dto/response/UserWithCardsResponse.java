package com.example.microservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserWithCardsResponse {

    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PaymentCardResponseDto> cards;
}
