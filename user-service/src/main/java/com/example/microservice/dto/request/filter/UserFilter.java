package com.example.microservice.dto.request.filter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFilter {
    private String name;
    private String surname;

    @Builder.Default
    @Min(value = 0, message = "Page must not be negative")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 20;

}
