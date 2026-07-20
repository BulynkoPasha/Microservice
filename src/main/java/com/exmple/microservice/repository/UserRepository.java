package com.exmple.microservice.repository;

import com.exmple.microservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Named method
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // JPQL
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.active = true")
    Optional<User> findActiveUserById(@Param("id") Long id);

    // Native SQL
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET active = :active WHERE id = :id", nativeQuery = true)
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

}
