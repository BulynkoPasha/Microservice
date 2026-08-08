package com.example.microservice.dto.request.filter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCardFilter {

    private String ownerName;
    private String ownerSurname;

    @Builder.Default
    @Min(0)
    private int page = 0;

    @Builder.Default
    @Min(1) @Max(100)
    private int size = 20;
}