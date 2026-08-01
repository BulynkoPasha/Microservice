package com.example.microservice.repository.specification;

import com.example.microservice.entity.PaymentCard;
import com.example.microservice.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {

    private PaymentCardSpecification() {
    }

    public static Specification<PaymentCard> filterByOwnerName(String name, String surname) {
        return (root, query, cb) -> {
            if ((name == null || name.isBlank()) && (surname == null || surname.isBlank())) {
                return cb.conjunction();
            }
            Join<PaymentCard, User> userJoin = root.join("user");
            var predicate = cb.conjunction();
            if (name != null && !name.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(userJoin.get("name")), name.toLowerCase()));
            }
            if (surname != null && !surname.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(userJoin.get("surname")), surname.toLowerCase()));
            }
            return predicate;
        };
    }
}