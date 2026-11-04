package com.restaurant.websocket;

import java.time.Instant;

public record OrderEvent(
        String type,
        Long orderId,
        String orderNumber,
        Long branchId,
        String status,
        Instant updatedAt
) {
}
