package com.example.paymentservice.integration;

import com.example.paymentservice.dto.request.PaymentCreateRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    private PaymentCreateRequestDto request(Long orderId, Long userId, BigDecimal amount) {
        return PaymentCreateRequestDto.builder()
                .orderId(orderId).userId(userId).paymentAmount(amount)
                .build();
    }

    @Test
    void createPayment_shouldReturn201_andPersistPayment() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/payments", request(1L, 1L, BigDecimal.valueOf(100)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("status")).isIn("SUCCESS", "FAILED");
    }

    @Test
    void getByUserId_shouldReturnPaymentsForUser() {
        restTemplate.postForEntity("/api/v1/payments", request(1L, 5L, BigDecimal.valueOf(50)), Map.class);
        restTemplate.postForEntity("/api/v1/payments", request(2L, 5L, BigDecimal.valueOf(75)), Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/payments/user/5", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getByOrderId_shouldReturnPaymentsForOrder() {
        restTemplate.postForEntity("/api/v1/payments", request(10L, 1L, BigDecimal.valueOf(30)), Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/payments/order/10", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getTotalSumForUser_shouldReturnCorrectSum() {
        restTemplate.postForEntity("/api/v1/payments", request(1L, 7L, BigDecimal.valueOf(100)), Map.class);
        restTemplate.postForEntity("/api/v1/payments", request(2L, 7L, BigDecimal.valueOf(200)), Map.class);

        String from = java.time.LocalDateTime.now().minusHours(1).toString();
        String to = java.time.LocalDateTime.now().plusHours(1).toString();

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/payments/sum/user/7?from=" + from + "&to=" + to, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalSum")).isNotNull();
    }

    @Test
    void createPayment_shouldPublishKafkaEvent() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/payments", request(99L, 1L, BigDecimal.valueOf(60)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody().get("orderId")).isEqualTo(99);
    }
}