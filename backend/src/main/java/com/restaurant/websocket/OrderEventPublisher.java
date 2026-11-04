package com.restaurant.websocket;

import com.restaurant.entity.CustomerOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tells the kitchen and the customer tracking page that an order changed.
 * The message is sent only after the database transaction commits.
 */
@Component
public class OrderEventPublisher {

    private final SimpMessagingTemplate messaging;

    public OrderEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void publishAfterCommit(CustomerOrder order, String type) {
        OrderEvent event = new OrderEvent(
                type,
                order.getId(),
                order.getOrderNumber(),
                order.getBranch().getId(),
                order.getStatus().name(),
                order.getUpdatedAt());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(OrderEvent event) {
        messaging.convertAndSend("/topic/kitchen/" + event.branchId(), event);
        messaging.convertAndSend("/topic/orders/" + event.orderId(), event);
    }
}
