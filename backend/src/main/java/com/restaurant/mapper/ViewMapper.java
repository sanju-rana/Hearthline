package com.restaurant.mapper;

import com.restaurant.dto.AuthDtos;
import com.restaurant.dto.CatalogDtos;
import com.restaurant.dto.OrderDtos;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.MenuCategory;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.OrderItem;
import com.restaurant.entity.OrderStatusHistory;
import com.restaurant.entity.Payment;
import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;

import java.util.List;

public final class ViewMapper {

    private ViewMapper() {
    }

    public static AuthDtos.UserResponse toUser(UserAccount user) {
        RestaurantBranch branch = user.getBranch();
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName());
    }

    public static CatalogDtos.RestaurantResponse toRestaurant(Restaurant restaurant) {
        return new CatalogDtos.RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getLogoUrl(),
                restaurant.isActive());
    }

    public static CatalogDtos.BranchResponse toBranch(RestaurantBranch branch) {
        return new CatalogDtos.BranchResponse(
                branch.getId(),
                branch.getRestaurant().getId(),
                branch.getRestaurant().getName(),
                branch.getName(),
                branch.getAddressLine(),
                branch.getCity(),
                branch.getState(),
                branch.getPostalCode(),
                branch.getPhone(),
                branch.getOpensAt(),
                branch.getClosesAt(),
                branch.isActive(),
                branch.isAcceptingOrders());
    }

    public static CatalogDtos.CategoryResponse toCategory(MenuCategory category) {
        return new CatalogDtos.CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.isActive());
    }

    public static CatalogDtos.MenuItemResponse toMenuItem(MenuItem item, boolean available) {
        return new CatalogDtos.MenuItemResponse(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isVegetarian(),
                item.getImageUrl(),
                available,
                item.isPopular(),
                item.isActive());
    }

    public static OrderDtos.OrderItemResponse toOrderItem(OrderItem item) {
        return new OrderDtos.OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.isVegetarian());
    }

    public static OrderDtos.PaymentResponse toPayment(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new OrderDtos.PaymentResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getProviderReference(),
                payment.getFailureReason());
    }

    public static OrderDtos.OrderSummaryResponse toSummary(CustomerOrder order, Payment payment) {
        return new OrderDtos.OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getOrderType(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                payment == null ? null : payment.getStatus(),
                order.getCreatedAt());
    }

    public static OrderDtos.KitchenOrderResponse toKitchen(CustomerOrder order) {
        return new OrderDtos.KitchenOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getStatus(),
                order.getSpecialInstructions(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getEstimatedPrepMinutes(),
                order.getCreatedAt(),
                order.getItems().stream().map(ViewMapper::toOrderItem).toList());
    }

    public static OrderDtos.OrderDetailResponse toDetail(CustomerOrder order) {
        List<OrderDtos.HistoryResponse> history = order.getHistory().stream()
                .map(ViewMapper::toHistory)
                .toList();
        return new OrderDtos.OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getOrderType(),
                order.getStatus(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getSpecialInstructions(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getEstimatedPrepMinutes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(ViewMapper::toOrderItem).toList(),
                toPayment(order.getPayment()),
                history);
    }

    private static OrderDtos.HistoryResponse toHistory(OrderStatusHistory entry) {
        return new OrderDtos.HistoryResponse(
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getNote(),
                entry.getCreatedAt());
    }
}
