package com.restaurant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Payment payment, Order order, Cors cors) {

    public record Jwt(String secret, int expirationMinutes, String cookieName, boolean cookieSecure) {
    }

    public record Payment(String mode, boolean allowSimulatedFailure) {
    }

    public record Order(
            BigDecimal taxRate,
            BigDecimal deliveryFee,
            String currency,
            int prepBaseMinutes,
            int prepPerItemMinutes,
            int prepMaxMinutes,
            String businessZone
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
