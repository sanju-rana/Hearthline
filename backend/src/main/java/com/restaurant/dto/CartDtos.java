package com.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record SetBranchRequest(@NotNull Long branchId) {
    }

    public record AddCartItemRequest(
            @NotNull Long menuItemId,
            @NotNull @Min(1) @Max(20) Integer quantity
    ) {
    }

    public record UpdateCartItemRequest(@NotNull @Min(1) @Max(20) Integer quantity) {
    }

    public record CartLineResponse(
            Long id,
            Long menuItemId,
            String name,
            String imageUrl,
            boolean vegetarian,
            boolean available,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public record CartResponse(
            CatalogDtos.BranchResponse branch,
            List<CartLineResponse> items,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal deliveryFeeIfDelivery,
            BigDecimal pickupTotal,
            BigDecimal deliveryTotal,
            String currency,
            int itemCount
    ) {
    }
}
