package com.restaurant.repository;

import com.restaurant.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
            select distinct c from Cart c
            left join fetch c.branch
            left join fetch c.items item
            left join fetch item.menuItem
            where c.customer.id = :customerId
            """)
    Optional<Cart> findDetailedByCustomerId(Long customerId);

    Optional<Cart> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.customer.id = :customerId")
    Optional<Cart> lockByCustomerId(Long customerId);
}
