package com.restaurant.service;

import com.restaurant.dto.OrderDtos;
import com.restaurant.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Idempotency {

    private Idempotency() {
    }

    public static void checkKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9_-]{8,80}$")) {
            throw new BadRequestException(
                    "Idempotency-Key must be 8 to 80 letters, numbers, hyphens, or underscores.");
        }
    }

    public static String fingerprint(OrderDtos.PlaceOrderRequest request) {
        String raw = String.join("|",
                request.orderType().name(),
                normalize(request.customerName()),
                request.phone().trim(),
                normalize(request.deliveryAddress()),
                normalize(request.specialInstructions()),
                request.paymentOutcome().name());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
