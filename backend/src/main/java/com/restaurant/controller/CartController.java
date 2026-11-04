package com.restaurant.controller;

import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.CartDtos;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartDtos.CartResponse> get(@AuthenticationPrincipal RestaurantUserDetails user) {
        return ApiResponse.ok(cartService.get(user.getId()));
    }

    @PutMapping("/branch")
    public ApiResponse<CartDtos.CartResponse> setBranch(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @Valid @RequestBody CartDtos.SetBranchRequest request
    ) {
        return ApiResponse.ok(cartService.setBranch(user.getId(), request.branchId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.CartResponse> add(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @Valid @RequestBody CartDtos.AddCartItemRequest request
    ) {
        return ApiResponse.ok(cartService.addItem(user.getId(), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartDtos.CartResponse> update(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartDtos.UpdateCartItemRequest request
    ) {
        return ApiResponse.ok(cartService.updateItem(user.getId(), cartItemId, request.quantity()));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartDtos.CartResponse> remove(
            @AuthenticationPrincipal RestaurantUserDetails user,
            @PathVariable Long cartItemId
    ) {
        return ApiResponse.ok(cartService.removeItem(user.getId(), cartItemId));
    }

    @DeleteMapping
    public ApiResponse<CartDtos.CartResponse> clear(@AuthenticationPrincipal RestaurantUserDetails user) {
        return ApiResponse.ok(cartService.clear(user.getId()));
    }
}
