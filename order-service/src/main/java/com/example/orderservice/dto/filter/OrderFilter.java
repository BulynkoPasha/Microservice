package com.example.orderservice.dto.filter;

import com.example.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilter {

    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private List<OrderStatus> statuses;

    @Builder.Default
    @Min(0)
    private int page = 0;

    @Builder.Default
    @Min(1)
    @Max(100)
    private int size = 20;

    @AssertTrue(message = "createdFrom must not be after createdTo")
    public boolean isDateRangeValid() {
        if (createdFrom == null || createdTo == null) {
            return true;
        }
        return !createdFrom.isAfter(createdTo);
    }
}