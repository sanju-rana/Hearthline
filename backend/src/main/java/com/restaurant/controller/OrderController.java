package com.restaurant.controller;

import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.OrderDtos;
import com.restaurant.dto.PageResponse;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDtos.OrderDetailResponse>> place(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @Valid @RequestBody OrderDtos.PlaceOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orderService.place(user.getId(), request, idempotencyKey)));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<OrderDtos.OrderSummaryResponse>> mine(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(orderService.myOrders(user.getId(), page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDtos.OrderDetailResponse> detail(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.ok(orderService.getOwn(user.getId(), orderId));
    }

    @GetMapping("/{orderId}/tracking")
    public ApiResponse<OrderDtos.OrderDetailResponse> tracking(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.ok(orderService.getOwn(user.getId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderDtos.OrderDetailResponse> cancel(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.ok(orderService.cancel(user.getId(), orderId));
    }
}
