package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.PaymentCreateRequestDto;
import com.example.paymentservice.dto.response.PaymentResponseDto;
import com.example.paymentservice.dto.response.TotalSumResponseDto;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentCreateRequestDto request) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getByUserId(userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDto>> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponseDto>> getByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getByStatus(status));
    }

    @GetMapping("/sum/user/{userId}")
    public ResponseEntity<TotalSumResponseDto> getTotalSumForUser(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getTotalSumForUser(userId, from, to));
    }

    @GetMapping("/sum/all")
    public ResponseEntity<TotalSumResponseDto> getTotalSumForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getTotalSumForAllUsers(from, to));
    }
}