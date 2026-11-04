package com.restaurant.service;

import com.restaurant.enums.OrderStatus;
import com.restaurant.exception.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStatusMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> KITCHEN_MOVES = Map.of(
            OrderStatus.PLACED, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED),
            OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.PREPARING),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY),
            OrderStatus.READY, EnumSet.of(OrderStatus.COMPLETED)
    );

    private OrderStatusMachine() {
    }

    public static void assertKitchenMove(OrderStatus from, OrderStatus to) {
        if (!KITCHEN_MOVES.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("This order cannot move from " + from + " to " + to + ".");
        }
    }

    public static void assertCustomerCancel(OrderStatus from) {
        if (from != OrderStatus.PLACED) {
            throw new ConflictException("Only a newly placed order can be cancelled.");
        }
    }
}
