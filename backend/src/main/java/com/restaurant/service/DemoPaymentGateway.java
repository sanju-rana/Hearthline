package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.enums.PaymentOutcome;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Demo checkout. This is not a card payment provider.
 * The UI and the stored provider name both say DEMO.
 */
@Service
public class DemoPaymentGateway implements PaymentGateway {

    private final AppProperties properties;

    public DemoPaymentGateway(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentCharge charge(BigDecimal amount, PaymentOutcome outcome) {
        if (!"demo".equalsIgnoreCase(properties.payment().mode())) {
            throw new IllegalStateException("Payment provider is not configured.");
        }
        if (outcome != PaymentOutcome.SUCCESS && !properties.payment().allowSimulatedFailure()) {
            throw new BadRequestException("This server is not accepting simulated payment failures.");
        }
        String reference = "DEMO-" + UUID.randomUUID();
        return switch (outcome) {
            case SUCCESS -> new PaymentCharge(PaymentStatus.SUCCESS, reference, null);
            case FAILURE -> new PaymentCharge(PaymentStatus.FAILED, reference, "The demo bank declined this payment.");
            case PENDING -> new PaymentCharge(PaymentStatus.PENDING, reference, "The demo bank has not confirmed this payment.");
        };
    }
}
