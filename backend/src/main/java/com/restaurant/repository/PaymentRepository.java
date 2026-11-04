package com.restaurant.repository;

import com.restaurant.entity.Payment;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdIn(Collection<Long> orderIds);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :status
              and p.order.status not in :excluded
            """)
    BigDecimal sumAmount(PaymentStatus status, Collection<OrderStatus> excluded);
}
