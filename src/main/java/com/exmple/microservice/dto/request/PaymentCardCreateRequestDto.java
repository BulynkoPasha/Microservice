package com.exmple.microservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentCardCreateRequestDto {

    @NotNull(message = "User id must not be null")
    private Long userId;

    @NotBlank(message = "Card number must not be blank")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain 13 to 19 digits")
    private String number;

    @NotBlank(message = "Holder must not be blank")
    @Size(max = 200)
    private String holder;

    @NotNull(message = "Expiration date must not be null")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;
}
