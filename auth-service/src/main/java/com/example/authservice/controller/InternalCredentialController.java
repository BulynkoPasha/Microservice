package com.example.authservice.controller;

import com.example.authservice.exception.InternalAccessDeniedException;
import com.example.authservice.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/credentials")
@RequiredArgsConstructor
public class InternalCredentialController {

    private final CredentialRepository credentialRepository;

    @Value("${internal.secret}")
    private String internalSecret;

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-Internal-Secret") String providedSecret) {

        if (!internalSecret.equals(providedSecret)) {
            throw new InternalAccessDeniedException("Invalid internal secret");
        }

        credentialRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}