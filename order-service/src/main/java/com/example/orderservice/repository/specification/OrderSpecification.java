package com.example.orderservice.repository.specification;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Order> hasStatuses(List<OrderStatus> statuses) {
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty()
                        ? cb.conjunction()
                        : root.get("status").in(statuses);
    }

    public static Specification<Order> filterBy(LocalDateTime from, LocalDateTime to, List<OrderStatus> statuses) {
        return Specification.where(createdBetween(from, to)).and(hasStatuses(statuses));
    }
}