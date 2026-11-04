package com.restaurant.service;

import com.restaurant.dto.OrderDtos;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.Payment;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.ForbiddenOperationException;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.websocket.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class KitchenService {

    private static final List<OrderStatus> ACTIVE = List.of(
            OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY);

    private final CustomerOrderRepository orders;
    private final UserAccountRepository users;
    private final OrderEventPublisher events;

    public KitchenService(
            CustomerOrderRepository orders,
            UserAccountRepository users,
            OrderEventPublisher events
    ) {
        this.orders = orders;
        this.users = users;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.KitchenOrderResponse> list(RestaurantUserDetails staff, OrderStatus status) {
        Long branchId = requiredBranch(staff);
        List<CustomerOrder> found = status == null
                ? orders.findKitchenActive(branchId, ACTIVE)
                : orders.findKitchenLane(branchId, status);
        return found.stream().map(ViewMapper::toKitchen).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.KitchenOrderResponse get(RestaurantUserDetails staff, Long orderId) {
        CustomerOrder order = findInBranch(staff, orderId);
        order.getItems().size();
        return ViewMapper.toKitchen(order);
    }

    @Transactional
    public OrderDtos.KitchenOrderResponse updateStatus(RestaurantUserDetails staff, Long orderId, OrderDtos.StatusChangeRequest request) {
        CustomerOrder order = findInBranch(staff, orderId);
        order.getItems().size();
        OrderStatusMachine.assertKitchenMove(order.getStatus(), request.status());
        UserAccount actor = users.findById(staff.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        OrderStatus from = order.getStatus();
        order.setStatus(request.status());
        order.setUpdatedAt(Instant.now());
        var entry = new com.restaurant.entity.OrderStatusHistory();
        entry.setChangedBy(actor);
        entry.setFromStatus(from);
        entry.setToStatus(request.status());
        entry.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        entry.setCreatedAt(order.getUpdatedAt());
        order.addHistory(entry);
        if (request.status() == OrderStatus.REJECTED) {
            Payment payment = order.getPayment();
            if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setFailureReason("Demo payment marked cancelled. No money was moved.");
            }
        }
        events.publishAfterCommit(order, "ORDER_UPDATED");
        return ViewMapper.toKitchen(order);
    }

    private CustomerOrder findInBranch(RestaurantUserDetails staff, Long orderId) {
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!requiredBranch(staff).equals(order.getBranch().getId())) {
            throw new ForbiddenOperationException("You are not authorized to access this order.");
        }
        return order;
    }

    private Long requiredBranch(RestaurantUserDetails staff) {
        if (staff.getBranchId() == null) {
            throw new ForbiddenOperationException("You are not authorized to access this order.");
        }
        return staff.getBranchId();
    }
}
