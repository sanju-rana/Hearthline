package com.restaurant.service;

import com.restaurant.enums.PaymentOutcome;
import com.restaurant.enums.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentCharge charge(BigDecimal amount, PaymentOutcome outcome);

    record PaymentCharge(PaymentStatus status, String reference, String failureReason) {
    }
}
