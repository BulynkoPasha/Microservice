package com.example.orderservice.consumer;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentCreatedEvent;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "${kafka.topic.payment-events}", groupId = "order-service-group")
    @Transactional
    public void handlePaymentCreatedEvent(PaymentCreatedEvent event) {
        log.info("Received CREATE_PAYMENT event for orderId={}, status={}",
                event.getOrderId(), event.getStatus());

        if (!"CREATE_PAYMENT".equals(event.getEventType())) {
            log.debug("Ignoring event of type {}", event.getEventType());
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresentOrElse(
                order -> updateOrderStatus(order, event.getStatus()),
                () -> log.warn("Order not found for orderId={}, cannot update status", event.getOrderId())
        );
    }

    private void updateOrderStatus(Order order, String paymentStatus) {
        OrderStatus newStatus = "SUCCESS".equals(paymentStatus)
                ? OrderStatus.PAID
                : OrderStatus.CANCELLED;

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order id={} status updated to {}", order.getId(), newStatus);
    }
}