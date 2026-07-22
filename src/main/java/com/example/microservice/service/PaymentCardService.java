package com.example.microservice.service;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface PaymentCardService {

    PaymentCardResponseDto createCard(PaymentCardCreateRequestDto requestDto);

    PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateRequestDto requestDto);

    PaymentCardResponseDto getCardById(Long id);

    PaymentCardResponseDto activateCard(Long id);

    PaymentCardResponseDto deactivateCard(Long id);

    List<PaymentCardResponseDto> getCardsByUserId(Long userId);

}
