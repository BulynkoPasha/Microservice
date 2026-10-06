package com.example.paymentservice.producer;

import com.example.paymentservice.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.payment-events}")
    private String topic;

    public void sendPaymentCreatedEvent(PaymentCreatedEvent event) {
        kafkaTemplate.send(topic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send CREATE_PAYMENT event for orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("Sent CREATE_PAYMENT event for orderId={}, status={}",
                                event.getOrderId(), event.getStatus());
                    }
                });
    }
}