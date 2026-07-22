package com.example.microservice.service.impl;

import com.example.microservice.config.CacheNames;
import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.service.PaymentCardService;
import com.example.microservice.entity.PaymentCard;
import com.example.microservice.entity.User;
import com.example.microservice.exception.CardLimitExceededException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.PaymentCardMapper;
import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentCardServiceImpl implements PaymentCardService {
    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;
    private final CacheManager cacheManager;


    @Override
    public PaymentCardResponseDto createCard(PaymentCardCreateRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestDto.getUserId()));

        long cardsCount = paymentCardRepository.countByUserId(requestDto.getUserId());
        if (cardsCount >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException(
                    "User with id " + requestDto.getUserId() + " already has maximum number of cards: " + MAX_CARDS_PER_USER);
        }

        PaymentCard card = paymentCardMapper.toEntity(requestDto);
        card.setUser(user);
        PaymentCard saved = paymentCardRepository.save(card);
        evictUserWithCardsCache(saved.getUser().getId());
        return paymentCardMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateRequestDto requestDto) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        paymentCardMapper.updateEntityFromRequest(requestDto, card);
        PaymentCard saved = paymentCardRepository.save(card);
        evictUserWithCardsCache(saved.getUser().getId());
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public PaymentCardResponseDto getCardById(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        evictUserWithCardsCache(card.getUser().getId());
        return paymentCardMapper.toDto(card);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto activateCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);
        evictUserWithCardsCache(saved.getUser().getId());
        return paymentCardMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto deactivateCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        card.setActive(false);
        PaymentCard saved = paymentCardRepository.save(card);
        evictUserWithCardsCache(saved.getUser().getId());
        return paymentCardMapper.toDto(saved);
    }

    @Override
    public List<PaymentCardResponseDto> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId).stream()
                .map(paymentCardMapper::toDto)
                .toList();
    }

    private void evictUserWithCardsCache(Long userId) {
        var cache = cacheManager.getCache(CacheNames.USERS_WITH_CARDS);
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
