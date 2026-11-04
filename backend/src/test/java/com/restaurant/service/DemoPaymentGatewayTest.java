package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.enums.PaymentOutcome;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPaymentGatewayTest {

    private final AppProperties properties = new AppProperties(
            new AppProperties.Jwt("test-only-hearthline-jwt-secret-32-characters", 30, "hearthline_token", false),
            new AppProperties.Payment("demo", true),
            new AppProperties.Order(new BigDecimal("0.05"), new BigDecimal("40.00"), "INR", 15, 2, 45, "Asia/Kolkata"),
            new AppProperties.Cors(List.of("http://localhost:5173"))
    );

    private final DemoPaymentGateway gateway = new DemoPaymentGateway(properties);

    @Test
    void successFailureAndPendingAreLabeledDemo() {
        PaymentGateway.PaymentCharge success = gateway.charge(new BigDecimal("100.00"), PaymentOutcome.SUCCESS);
        PaymentGateway.PaymentCharge failed = gateway.charge(new BigDecimal("100.00"), PaymentOutcome.FAILURE);
        PaymentGateway.PaymentCharge pending = gateway.charge(new BigDecimal("100.00"), PaymentOutcome.PENDING);

        assertEquals(PaymentStatus.SUCCESS, success.status());
        assertEquals(PaymentStatus.FAILED, failed.status());
        assertEquals(PaymentStatus.PENDING, pending.status());
        assertTrue(success.reference().startsWith("DEMO-"));
        assertTrue(failed.reference().startsWith("DEMO-"));
        assertTrue(pending.reference().startsWith("DEMO-"));
    }

    @Test
    void simulatedFailureCanBeTurnedOff() {
        DemoPaymentGateway strict = new DemoPaymentGateway(new AppProperties(
                properties.jwt(),
                new AppProperties.Payment("demo", false),
                properties.order(),
                properties.cors()));
        assertThrows(BadRequestException.class, () -> strict.charge(new BigDecimal("10.00"), PaymentOutcome.FAILURE));
    }
}
