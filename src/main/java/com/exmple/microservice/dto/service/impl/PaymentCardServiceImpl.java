package com.exmple.microservice.dto.service.impl;

import com.exmple.microservice.dto.request.PaymentCardCreateRequestDto;
import com.exmple.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.exmple.microservice.dto.response.PaymentCardResponseDto;
import com.exmple.microservice.dto.service.PaymentCardService;
import com.exmple.microservice.entity.PaymentCard;
import com.exmple.microservice.entity.User;
import com.exmple.microservice.mapper.PaymentCardMapper;
import com.exmple.microservice.repository.PaymentCardRepository;
import com.exmple.microservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentCardServiceImpl implements PaymentCardService {
    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;


    @Override
    public PaymentCardResponseDto createCard(PaymentCardCreateRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        long cardsCount = paymentCardRepository.countByUserId(requestDto.getUserId());
        if (cardsCount >= MAX_CARDS_PER_USER) {
            throw new IllegalStateException(
                    "User with id " + requestDto.getUserId() + " already has maximum number of cards: " + MAX_CARDS_PER_USER);
        }

        PaymentCard card = paymentCardMapper.toEntity(requestDto);
        card.setUser(user);
        PaymentCard saved = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateRequestDto requestDto) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));
        paymentCardMapper.updateEntityFromRequest(requestDto, card);
        PaymentCard saved = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public PaymentCardResponseDto getCardById(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));
        return paymentCardMapper.toDto(card);
    }

    @Override
    public PaymentCardResponseDto activateCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public PaymentCardResponseDto deactivateCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));
        card.setActive(false);
        PaymentCard saved = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public List<PaymentCardResponseDto> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId).stream()
                .map(paymentCardMapper::toDto)
                .toList();
    }
}
