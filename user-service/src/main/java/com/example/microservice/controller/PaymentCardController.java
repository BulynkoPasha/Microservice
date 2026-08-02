package com.example.microservice.controller;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.request.filter.PaymentCardFilter;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.service.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #request.userId == authentication.principal.userId")
    public ResponseEntity<PaymentCardResponseDto> createCard(@Valid @RequestBody PaymentCardCreateRequestDto request) {
        PaymentCardResponseDto response = paymentCardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication.principal.userId)")
    public ResponseEntity<PaymentCardResponseDto> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getCardById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<PaymentCardResponseDto>> getCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication.principal.userId)")
    public ResponseEntity<PaymentCardResponseDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardUpdateRequestDto request) {
        return ResponseEntity.ok(paymentCardService.updateCard(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication.principal.userId)")
    public ResponseEntity<PaymentCardResponseDto> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.activateCard(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication.principal.userId)")
    public ResponseEntity<PaymentCardResponseDto> deactivateCard(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.deactivateCard(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllCards(
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String ownerSurname,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PaymentCardFilter filter = PaymentCardFilter.builder()
                .ownerName(ownerName).ownerSurname(ownerSurname)
                .page(page).size(size).build();
        return ResponseEntity.ok(paymentCardService.getAllCards(filter));
    }

    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<PaymentCardResponseDto>> getActiveCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getActiveCardsByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication.principal.userId)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
