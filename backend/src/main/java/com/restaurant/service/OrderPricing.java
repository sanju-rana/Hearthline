package com.restaurant.service;

import com.restaurant.enums.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderPricing {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private OrderPricing() {
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return money(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public static BigDecimal tax(BigDecimal subtotal, BigDecimal rate) {
        return money(subtotal.multiply(rate));
    }

    public static BigDecimal deliveryFee(OrderType orderType, BigDecimal configuredFee) {
        if (orderType == OrderType.DELIVERY) {
            return money(configuredFee);
        }
        return new BigDecimal("0.00");
    }

    public static BigDecimal total(BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryFee) {
        return money(subtotal.add(tax).add(deliveryFee));
    }
}
