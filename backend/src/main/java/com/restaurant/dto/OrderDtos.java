package com.restaurant.dto;

import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.PaymentOutcome;
import com.restaurant.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotNull OrderType orderType,
            @NotBlank @Size(max = 80) String customerName,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit phone number.") String phone,
            @Size(max = 300) String deliveryAddress,
            @Size(max = 300) String specialInstructions,
            @NotNull PaymentOutcome paymentOutcome
    ) {
    }

    public record StatusChangeRequest(@NotNull OrderStatus status, @Size(max = 300) String note) {
    }

    public record OrderItemResponse(
            Long id,
            Long menuItemId,
            String itemName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            boolean vegetarian
    ) {
    }

    public record PaymentResponse(
            Long id,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            String provider,
            String providerReference,
            String failureReason
    ) {
    }

    public record HistoryResponse(OrderStatus fromStatus, OrderStatus toStatus, String note, Instant createdAt) {
    }

    public record KitchenOrderResponse(
            Long id,
            String orderNumber,
            OrderType orderType,
            OrderStatus status,
            String specialInstructions,
            BigDecimal totalAmount,
            String currency,
            int estimatedPrepMinutes,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {
    }

    public record OrderSummaryResponse(
            Long id,
            String orderNumber,
            Long branchId,
            String branchName,
            OrderType orderType,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            PaymentStatus paymentStatus,
            Instant createdAt
    ) {
    }

    public record OrderDetailResponse(
            Long id,
            String orderNumber,
            Long branchId,
            String branchName,
            OrderType orderType,
            OrderStatus status,
            String customerName,
            String customerPhone,
            String deliveryAddress,
            String specialInstructions,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal deliveryFee,
            BigDecimal totalAmount,
            String currency,
            int estimatedPrepMinutes,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItemResponse> items,
            PaymentResponse payment,
            List<HistoryResponse> history
    ) {
    }
}
