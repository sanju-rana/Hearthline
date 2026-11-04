package com.restaurant.dto;

import com.restaurant.enums.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record StatusCount(OrderStatus status, long count) {
    }

    public record DashboardResponse(
            long totalOrders,
            long todaysOrders,
            long pendingOrders,
            long completedOrders,
            BigDecimal revenue,
            long activeBranches,
            long availableMenuItems,
            List<StatusCount> ordersByStatus
    ) {
    }

    public record BranchWriteRequest(
            @NotNull Long restaurantId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 200) String addressLine,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 12) String postalCode,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit phone number.") String phone,
            @NotNull LocalTime opensAt,
            @NotNull LocalTime closesAt,
            boolean active,
            boolean acceptingOrders
    ) {
    }

    public record BranchOperationsRequest(boolean active, boolean acceptingOrders) {
    }

    public record CategoryWriteRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 200) String description,
            @NotNull @Min(0) Integer displayOrder,
            boolean active
    ) {
    }

    public record MenuItemWriteRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
            boolean vegetarian,
            boolean popular,
            @Size(max = 500) String imageUrl,
            boolean active
    ) {
    }

    public record AvailabilityRequest(boolean available) {
    }

    public record BranchAvailability(Long branchId, String branchName, boolean available) {
    }

    public record AdminMenuItem(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String description,
            BigDecimal price,
            boolean vegetarian,
            boolean popular,
            String imageUrl,
            boolean active,
            List<BranchAvailability> availability
    ) {
    }

    public record AdminMenuResponse(
            List<CatalogDtos.CategoryResponse> categories,
            List<AdminMenuItem> items,
            List<CatalogDtos.BranchResponse> branches
    ) {
    }

    public record StaffRequest(
            @NotBlank @Size(max = 80) String fullName,
            @Email @NotBlank @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit phone number.") String phone,
            @NotNull Long branchId
    ) {
    }
}
