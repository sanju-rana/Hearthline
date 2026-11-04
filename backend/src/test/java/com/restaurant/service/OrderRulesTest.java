package com.restaurant.service;

import com.restaurant.dto.OrderDtos;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.PaymentOutcome;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderRulesTest {

    @Test
    void kitchenMovesFollowTheLifecycle() {
        OrderStatusMachine.assertKitchenMove(OrderStatus.PLACED, OrderStatus.ACCEPTED);
        OrderStatusMachine.assertKitchenMove(OrderStatus.PLACED, OrderStatus.REJECTED);
        OrderStatusMachine.assertKitchenMove(OrderStatus.ACCEPTED, OrderStatus.PREPARING);
        OrderStatusMachine.assertKitchenMove(OrderStatus.PREPARING, OrderStatus.READY);
        OrderStatusMachine.assertKitchenMove(OrderStatus.READY, OrderStatus.COMPLETED);
    }

    @Test
    void illegalKitchenMovesAreRejected() {
        assertThrows(ConflictException.class, () -> OrderStatusMachine.assertKitchenMove(OrderStatus.PLACED, OrderStatus.READY));
        assertThrows(ConflictException.class, () -> OrderStatusMachine.assertKitchenMove(OrderStatus.PREPARING, OrderStatus.COMPLETED));
        assertThrows(ConflictException.class, () -> OrderStatusMachine.assertKitchenMove(OrderStatus.COMPLETED, OrderStatus.PREPARING));
        assertThrows(ConflictException.class, () -> OrderStatusMachine.assertCustomerCancel(OrderStatus.PREPARING));
    }

    @Test
    void customerCanCancelOnlyAPlacedOrder() {
        OrderStatusMachine.assertCustomerCancel(OrderStatus.PLACED);
    }

    @Test
    void pricesUseDatabaseMathAndHalfUpRounding() {
        assertEquals(new BigDecimal("798.00"), OrderPricing.lineTotal(new BigDecimal("399.00"), 2));
        assertEquals(new BigDecimal("19.95"), OrderPricing.tax(new BigDecimal("399.00"), new BigDecimal("0.05")));
        assertEquals(new BigDecimal("40.00"), OrderPricing.deliveryFee(OrderType.DELIVERY, new BigDecimal("40")));
        assertEquals(new BigDecimal("0.00"), OrderPricing.deliveryFee(OrderType.PICKUP, new BigDecimal("40")));
        assertEquals(new BigDecimal("458.95"), OrderPricing.total(
                new BigDecimal("399.00"), new BigDecimal("19.95"), new BigDecimal("40.00")));
    }

    @Test
    void prepEstimateIsCapped() {
        assertEquals(19, PrepTimeEstimator.estimate(2, 15, 2, 45));
        assertEquals(45, PrepTimeEstimator.estimate(40, 15, 2, 45));
    }

    @Test
    void passwordMustContainALetterAndANumber() {
        PasswordRules.check("Kitchen@123");
        assertThrows(BadRequestException.class, () -> PasswordRules.check("password"));
        assertThrows(BadRequestException.class, () -> PasswordRules.check("12345678"));
    }

    @Test
    void idempotencyFingerprintChangesWhenTheOrderChanges() {
        OrderDtos.PlaceOrderRequest first = request(OrderType.PICKUP, null);
        OrderDtos.PlaceOrderRequest second = request(OrderType.DELIVERY, "221 Lake View");
        assertEquals(Idempotency.fingerprint(first), Idempotency.fingerprint(request(OrderType.PICKUP, null)));
        assertNotEquals(Idempotency.fingerprint(first), Idempotency.fingerprint(second));
        Idempotency.checkKey("checkout-key-001");
        assertThrows(BadRequestException.class, () -> Idempotency.checkKey("short"));
    }

    private OrderDtos.PlaceOrderRequest request(OrderType type, String address) {
        return new OrderDtos.PlaceOrderRequest(type, "Asha Raman", "9845001122", address, "", PaymentOutcome.SUCCESS);
    }
}
