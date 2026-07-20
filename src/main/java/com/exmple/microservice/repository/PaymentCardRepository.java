package com.exmple.microservice.repository;

import com.exmple.microservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

    // Named method
    List<PaymentCard> findByUserId(Long userId);

    long countByUserId(Long userId);

    // JPQL
    @Query("SELECT c FROM PaymentCard c WHERE c.user.id = :userId AND c.active = true")
    List<PaymentCard> findActiveCardsByUserId(@Param("userId") Long userId);

    // Native SQL
    @Modifying
    @Transactional
    @Query(value = "UPDATE payment_cards SET active = :active WHERE id = :id", nativeQuery = true)
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
