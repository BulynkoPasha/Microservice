package com.example.microservice.service;

import com.example.microservice.config.CacheNames;
import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.entity.PaymentCard;
import com.example.microservice.entity.User;
import com.example.microservice.exception.CardLimitExceededException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.PaymentCardMapper;
import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.UserRepository;
import com.example.microservice.service.impl.PaymentCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;

    private User user;
    private PaymentCard card;
    private PaymentCardResponseDto cardResponse;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Ivan").build();
        card = PaymentCard.builder()
                .id(10L).user(user).number("4111111111111111")
                .holder("IVAN PETROV").expirationDate(LocalDate.now().plusYears(2))
                .active(true).build();
        cardResponse = PaymentCardResponseDto.builder().id(10L).userId(1L).build();
    }

    @Test
    void createCard_shouldSaveAndEvictCache_whenUnderLimit() {
        PaymentCardCreateRequestDto request = PaymentCardCreateRequestDto.builder()
                .userId(1L).number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(2)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(1L)).thenReturn(2L);
        when(paymentCardMapper.toEntity(request)).thenReturn(card);
        when(userRepository.save(user)).thenReturn(user);
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);
        when(cacheManager.getCache(CacheNames.USERS_WITH_CARDS)).thenReturn(cache);

        PaymentCardResponseDto result = paymentCardService.createCard(request);

        assertThat(result.getUserId()).isEqualTo(1L);
        verify(cache).evict(1L);
    }

    @Test
    void createCard_shouldThrowCardLimitExceededException_whenLimitReached() {
        PaymentCardCreateRequestDto request = PaymentCardCreateRequestDto.builder()
                .userId(1L).number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(2)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.createCard(request))
                .isInstanceOf(CardLimitExceededException.class);

        verify(paymentCardRepository, never()).save(any());
    }

    @Test
    void createCard_shouldThrowResourceNotFoundException_whenUserMissing() {
        PaymentCardCreateRequestDto request = PaymentCardCreateRequestDto.builder()
                .userId(99L).number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(2)).build();

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.createCard(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCardById_shouldReturnResponse_whenCardExists() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);

        PaymentCardResponseDto result = paymentCardService.getCardById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getCardById_shouldThrowResourceNotFoundException_whenCardMissing() {
        when(paymentCardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.getCardById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCardsByUserId_shouldReturnMappedList() {
        when(paymentCardRepository.findByUserId(1L)).thenReturn(List.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);

        List<PaymentCardResponseDto> result = paymentCardService.getCardsByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void updateCard_shouldUpdateAndEvictCache_whenCardExists() {
        PaymentCardUpdateRequestDto request = PaymentCardUpdateRequestDto.builder()
                .number("4111111111111111").holder("IVAN PETROV")
                .expirationDate(LocalDate.now().plusYears(3)).build();

        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);
        when(cacheManager.getCache(CacheNames.USERS_WITH_CARDS)).thenReturn(cache);

        paymentCardService.updateCard(10L, request);

        verify(paymentCardMapper).updateEntityFromRequest(request, card);
        verify(cache).evict(1L);
    }

    @Test
    void activateCard_shouldSetActiveTrueAndEvictCache() {
        card.setActive(false);
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);
        when(cacheManager.getCache(CacheNames.USERS_WITH_CARDS)).thenReturn(cache);

        paymentCardService.activateCard(10L);

        assertThat(card.isActive()).isTrue();
        verify(cache).evict(1L);
    }

    @Test
    void deactivateCard_shouldSetActiveFalseAndEvictCache() {
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardResponse);
        when(cacheManager.getCache(CacheNames.USERS_WITH_CARDS)).thenReturn(cache);

        paymentCardService.deactivateCard(10L);

        assertThat(card.isActive()).isFalse();
        verify(cache).evict(1L);
    }
}