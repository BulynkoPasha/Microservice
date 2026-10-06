package com.example.paymentservice.service.impl;

import com.example.paymentservice.client.RandomNumberClient;
import com.example.paymentservice.dto.request.PaymentCreateRequestDto;
import com.example.paymentservice.dto.response.PaymentResponseDto;
import com.example.paymentservice.dto.response.TotalSumResponseDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.event.PaymentCreatedEvent;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.producer.PaymentEventProducer;
import com.example.paymentservice.repository.PaymentAggregationRepository;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAggregationRepository paymentAggregationRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    public PaymentResponseDto createPayment(PaymentCreateRequestDto request) {
        Payment payment = paymentMapper.toEntity(request);
        payment.setTimestamp(LocalDateTime.now());
        payment.setStatus(resolveStatus());

        Payment saved = paymentRepository.save(payment);

        paymentEventProducer.sendPaymentCreatedEvent(
                PaymentCreatedEvent.builder()
                        .eventType("CREATE_PAYMENT")
                        .orderId(saved.getOrderId())
                        .paymentId(saved.getId())
                        .status(saved.getStatus())
                        .timestamp(saved.getTimestamp())
                        .build()
        );

        return paymentMapper.toResponse(saved);
    }

    @Override
    public List<PaymentResponseDto> getByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public TotalSumResponseDto getTotalSumForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        BigDecimal total = paymentAggregationRepository.sumByUserAndDateRange(userId, from, to);

        return TotalSumResponseDto.builder()
                .totalSum(total)
                .from(from)
                .to(to)
                .build();
    }

    @Override
    public TotalSumResponseDto getTotalSumForAllUsers(LocalDateTime from, LocalDateTime to) {
        BigDecimal total = paymentAggregationRepository.sumByDateRange(from, to);

        return TotalSumResponseDto.builder()
                .totalSum(total)
                .from(from)
                .to(to)
                .build();
    }

    private PaymentStatus resolveStatus() {
        int randomNumber = randomNumberClient.generateRandomNumber();
        return randomNumber % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}