package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Repository
@RequiredArgsConstructor
public class PaymentAggregationRepository {

    private final MongoTemplate mongoTemplate;

    public BigDecimal sumByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        MatchOperation match = match(
                org.springframework.data.mongodb.core.query.Criteria
                        .where("userId").is(userId)
                        .and("timestamp").gte(from).lte(to));

        return runSumAggregation(match);
    }

    public BigDecimal sumByDateRange(LocalDateTime from, LocalDateTime to) {
        MatchOperation match = match(
                org.springframework.data.mongodb.core.query.Criteria
                        .where("timestamp").gte(from).lte(to));

        return runSumAggregation(match);
    }

    private BigDecimal runSumAggregation(MatchOperation match) {
        GroupOperation group = group().sum("paymentAmount").as("total");

        Aggregation aggregation = newAggregation(match, group);

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, Payment.class, Map.class);

        Map result = results.getUniqueMappedResult();
        if (result == null || result.get("total") == null) {
            return BigDecimal.ZERO;
        }

        Object total = result.get("total");
        if (total instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(total.toString());
    }
}