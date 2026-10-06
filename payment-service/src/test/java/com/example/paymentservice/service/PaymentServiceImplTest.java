package com.example.paymentservice.service;

import com.example.paymentservice.client.RandomNumberClient;
import com.example.paymentservice.dto.request.PaymentCreateRequestDto;
import com.example.paymentservice.dto.response.PaymentResponseDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.producer.PaymentEventProducer;
import com.example.paymentservice.repository.PaymentAggregationRepository;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentAggregationRepository paymentAggregationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentResponseDto paymentResponse;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id("1").orderId(100L).userId(1L)
                .paymentAmount(BigDecimal.valueOf(50))
                .build();

        paymentResponse = PaymentResponseDto.builder()
                .id("1").orderId(100L).userId(1L)
                .paymentAmount(BigDecimal.valueOf(50))
                .build();
    }

    @Test
    void createPayment_shouldSetStatusSuccess_whenRandomNumberIsEven() {
        PaymentCreateRequestDto request = PaymentCreateRequestDto.builder()
                .orderId(100L).userId(1L).paymentAmount(BigDecimal.valueOf(50))
                .build();

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomNumberClient.generateRandomNumber()).thenReturn(4);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentEventProducer).sendPaymentCreatedEvent(any());
    }

    @Test
    void createPayment_shouldSetStatusFailed_whenRandomNumberIsOdd() {
        PaymentCreateRequestDto request = PaymentCreateRequestDto.builder()
                .orderId(100L).userId(1L).paymentAmount(BigDecimal.valueOf(50))
                .build();

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomNumberClient.generateRandomNumber()).thenReturn(7);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentEventProducer).sendPaymentCreatedEvent(any());
    }

    @Test
    void createPayment_shouldSetTimestamp() {
        PaymentCreateRequestDto request = PaymentCreateRequestDto.builder()
                .orderId(100L).userId(1L).paymentAmount(BigDecimal.valueOf(50))
                .build();

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomNumberClient.generateRandomNumber()).thenReturn(2);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        assertThat(payment.getTimestamp()).isNotNull();
    }

    @Test
    void getByUserId_shouldReturnMappedList() {
        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponseDto> result = paymentService.getByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getByOrderId_shouldReturnMappedList() {
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponseDto> result = paymentService.getByOrderId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(100L);
    }

    @Test
    void getByStatus_shouldReturnMappedList() {
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponseDto> result = paymentService.getByStatus(PaymentStatus.SUCCESS);

        assertThat(result).hasSize(1);
    }

    @Test
    void getTotalSumForUser_shouldReturnSum_whenPaymentsExist() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(paymentAggregationRepository.sumByUserAndDateRange(1L, from, to))
                .thenReturn(BigDecimal.valueOf(150));

        var result = paymentService.getTotalSumForUser(1L, from, to);

        assertThat(result.getTotalSum()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void getTotalSumForUser_shouldReturnZero_whenNoPaymentsFound() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(paymentAggregationRepository.sumByUserAndDateRange(1L, from, to))
                .thenReturn(BigDecimal.ZERO);

        var result = paymentService.getTotalSumForUser(1L, from, to);

        assertThat(result.getTotalSum()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSumForAllUsers_shouldReturnSum_whenPaymentsExist() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(paymentAggregationRepository.sumByDateRange(from, to))
                .thenReturn(BigDecimal.valueOf(500));

        var result = paymentService.getTotalSumForAllUsers(from, to);

        assertThat(result.getTotalSum()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }
}