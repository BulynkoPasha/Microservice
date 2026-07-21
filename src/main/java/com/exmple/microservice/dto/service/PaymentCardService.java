package com.exmple.microservice.dto.service;

import com.exmple.microservice.dto.request.PaymentCardCreateRequestDto;
import com.exmple.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.exmple.microservice.dto.response.PaymentCardResponseDto;

import java.util.List;

public interface PaymentCardService {

    PaymentCardResponseDto createCard(PaymentCardCreateRequestDto requestDto);

    PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateRequestDto requestDto);

    PaymentCardResponseDto getCardById(Long id);

    PaymentCardResponseDto activateCard(Long id);

    PaymentCardResponseDto deactivateCard(Long id);

    List<PaymentCardResponseDto> getCardsByUserId(Long userId);

}
