package com.restaurant.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record RestaurantResponse(Long id, String name, String description, String logoUrl, boolean active) {
    }

    public record BranchResponse(
            Long id,
            Long restaurantId,
            String restaurantName,
            String name,
            String addressLine,
            String city,
            String state,
            String postalCode,
            String phone,
            LocalTime opensAt,
            LocalTime closesAt,
            boolean active,
            boolean acceptingOrders
    ) {
    }

    public record CategoryResponse(Long id, String name, String description, int displayOrder, boolean active) {
    }

    public record MenuItemResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String description,
            BigDecimal price,
            boolean vegetarian,
            String imageUrl,
            boolean available,
            boolean popular,
            boolean active
    ) {
    }
}
