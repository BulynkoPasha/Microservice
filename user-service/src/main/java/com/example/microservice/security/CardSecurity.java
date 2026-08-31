package com.example.microservice.security;

import com.example.microservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardSecurity {

    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long cardId, Long userId) {
        return paymentCardRepository.findById(cardId)
                .map(card -> card.getUser().getId().equals(userId))
                .orElse(false);
    }
}