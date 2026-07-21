package com.exmple.microservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequestDto {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Surname must not be blank")
    @Size(max = 200)
    private String surname;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Size(max = 200)
    private String email;
}
