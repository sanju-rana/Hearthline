package com.restaurant.exception;

import org.springframework.http.HttpStatus;

public class PaymentDeclinedException extends ApiException {

    public PaymentDeclinedException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
