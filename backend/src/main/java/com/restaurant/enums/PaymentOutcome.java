package com.restaurant.enums;

/**
 * Requested result of the demo payment gateway.
 * A real gateway would ignore this and return its own result.
 */
public enum PaymentOutcome {
    SUCCESS,
    FAILURE,
    PENDING
}
