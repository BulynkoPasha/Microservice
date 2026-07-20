package com.exmple.microservice.repository.specification;

import com.exmple.microservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> filterBy(String name, String surname) {
        return Specification.where(hasName(name)).and(hasSurname(surname));
    }

    public static Specification<User> hasName(String name) {
        return (root, query, cb) ->
                name == null || name.isBlank()
                        ? null
                        : cb.equal(cb.lower(root.get("name")), name.toLowerCase());
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) ->
                surname == null || surname.isBlank()
                        ? null
                        : cb.equal(cb.lower(root.get("surname")), surname.toLowerCase());
    }
}
