package com.example.paymentservice.event;

import com.example.paymentservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreatedEvent {

    private String eventType;
    private Long orderId;
    private String paymentId;
    private PaymentStatus status;
    private LocalDateTime timestamp;
}