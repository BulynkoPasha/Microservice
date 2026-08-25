package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0, 'timestamp': { '$gte': ?1, '$lte': ?2 } } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$paymentAmount' } } }"
    })
    Optional<TotalSum> sumPaymentsByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to);

    @Aggregation(pipeline = {
            "{ '$match': { 'timestamp': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$paymentAmount' } } }"
    })
    Optional<TotalSum> sumAllPaymentsByDateRange(LocalDateTime from, LocalDateTime to);

    interface TotalSum {
        BigDecimal getTotal();
    }
}
