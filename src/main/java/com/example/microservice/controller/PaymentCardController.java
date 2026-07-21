package com.example.microservice.controller;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.dto.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDto> createCard(@Valid @RequestBody PaymentCardCreateRequestDto request) {
        PaymentCardResponseDto response = paymentCardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getCardById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardResponseDto>> getCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardUpdateRequestDto request) {
        return ResponseEntity.ok(paymentCardService.updateCard(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PaymentCardResponseDto> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.activateCard(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PaymentCardResponseDto> deactivateCard(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.deactivateCard(id));
    }
}
