package com.exmple.microservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentCardResponseDto {
    private Long id;
    private Long userId;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
