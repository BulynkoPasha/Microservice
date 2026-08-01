package com.example.microservice.service.impl;

import com.example.microservice.config.CacheNames;
import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.request.filter.PaymentCardFilter;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.entity.PaymentCard;
import com.example.microservice.entity.User;
import com.example.microservice.exception.CardLimitExceededException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.PaymentCardMapper;
import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.UserRepository;
import com.example.microservice.repository.specification.PaymentCardSpecification;
import com.example.microservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    @Transactional
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
        user.getCards().add(card);
        User savedUser = userRepository.save(user);

        PaymentCard savedCard = savedUser.getCards().get(savedUser.getCards().size() - 1);
        evictUserWithCardsCache(savedUser.getId());
        return paymentCardMapper.toDto(savedCard);
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

    @Override
    @Transactional
    public void deleteCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));
        Long userId = card.getUser().getId();
        paymentCardRepository.delete(card);
        evictUserWithCardsCache(userId);
    }

    private void evictUserWithCardsCache(Long userId) {
        var cache = cacheManager.getCache(CacheNames.USERS_WITH_CARDS);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    @Override
    public Page<PaymentCardResponseDto> getAllCards(PaymentCardFilter filter) {
        Page<PaymentCard> page = paymentCardRepository.findAll(
                PaymentCardSpecification.filterByOwnerName(filter.getOwnerName(), filter.getOwnerSurname()),
                PageRequest.of(filter.getPage(), filter.getSize())
        );
        return page.map(paymentCardMapper::toDto);
    }

    @Override
    public List<PaymentCardResponseDto> getActiveCardsByUserId(Long userId) {
        return paymentCardRepository.findActiveCardsByUserId(userId).stream()
                .map(paymentCardMapper::toDto)
                .toList();
    }
}
