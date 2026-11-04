package com.restaurant.controller;

import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.OrderDtos;
import com.restaurant.enums.OrderStatus;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.service.KitchenService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen/orders")
@PreAuthorize("hasRole('KITCHEN_STAFF')")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping
    public ApiResponse<List<OrderDtos.KitchenOrderResponse>> list(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @RequestParam(required = false) OrderStatus status
    ) {
        return ApiResponse.ok(kitchenService.list(user, status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDtos.KitchenOrderResponse> get(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.ok(kitchenService.get(user, orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderDtos.KitchenOrderResponse> updateStatus(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderDtos.StatusChangeRequest request
    ) {
        return ApiResponse.ok(kitchenService.updateStatus(user, orderId, request));
    }
}
