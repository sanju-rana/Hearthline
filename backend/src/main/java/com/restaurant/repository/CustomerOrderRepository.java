package com.restaurant.repository;

import com.restaurant.entity.CustomerOrder;
import com.restaurant.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    Page<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @Query("""
            select distinct o from CustomerOrder o
            join fetch o.items
            join fetch o.branch
            where o.branch.id = :branchId and o.status = :status
            order by o.createdAt asc
            """)
    List<CustomerOrder> findKitchenLane(Long branchId, OrderStatus status);

    @Query("""
            select distinct o from CustomerOrder o
            join fetch o.items
            join fetch o.branch
            where o.branch.id = :branchId and o.status in :statuses
            order by o.createdAt asc
            """)
    List<CustomerOrder> findKitchenActive(Long branchId, Collection<OrderStatus> statuses);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    long countByCreatedAtGreaterThanEqual(Instant start);

    long countByStatusIn(Collection<OrderStatus> statuses);

    @Query("select o.status, count(o) from CustomerOrder o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
            select o from CustomerOrder o
            where (:status is null or o.status = :status)
              and (:branchId is null or o.branch.id = :branchId)
              and (
                    :orderNumber is null
                    or lower(o.orderNumber) like lower(concat('%', cast(:orderNumber as string), '%'))
                  )
            """)
    Page<CustomerOrder> search(OrderStatus status, Long branchId, String orderNumber, Pageable pageable);
}
