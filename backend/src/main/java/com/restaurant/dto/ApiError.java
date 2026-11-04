package com.restaurant.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(boolean success, String message, List<FieldViolation> errors, Instant timestamp) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String message) {
        return new ApiError(false, message, List.of(), Instant.now());
    }

    public static ApiError of(String message, List<FieldViolation> errors) {
        return new ApiError(false, message, errors, Instant.now());
    }
}
