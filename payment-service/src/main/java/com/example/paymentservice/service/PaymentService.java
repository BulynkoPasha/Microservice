package com.example.paymentservice.service;

import com.example.paymentservice.dto.request.PaymentCreateRequestDto;
import com.example.paymentservice.dto.response.PaymentResponseDto;
import com.example.paymentservice.dto.response.TotalSumResponseDto;
import com.example.paymentservice.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentCreateRequestDto request);

    List<PaymentResponseDto> getByUserId(Long userId);

    List<PaymentResponseDto> getByOrderId(Long orderId);

    List<PaymentResponseDto> getByStatus(PaymentStatus status);

    TotalSumResponseDto getTotalSumForUser(Long userId, LocalDateTime from, LocalDateTime to);

    TotalSumResponseDto getTotalSumForAllUsers(LocalDateTime from, LocalDateTime to);
}