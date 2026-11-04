package com.restaurant.controller;

import com.restaurant.dto.AdminDtos;
import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.AuthDtos;
import com.restaurant.dto.CatalogDtos;
import com.restaurant.dto.OrderDtos;
import com.restaurant.dto.PageResponse;
import com.restaurant.enums.OrderStatus;
import com.restaurant.service.AdminCatalogService;
import com.restaurant.service.AdminQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('RESTAURANT_ADMIN')")
public class AdminController {

    private final AdminQueryService queries;
    private final AdminCatalogService catalog;

    public AdminController(AdminQueryService queries, AdminCatalogService catalog) {
        this.queries = queries;
        this.catalog = catalog;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDtos.DashboardResponse> dashboard() {
        return ApiResponse.ok(queries.dashboard());
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderDtos.OrderSummaryResponse>> orders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(queries.orders(status, branchId, orderNumber, page, size));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderDtos.OrderDetailResponse> order(@PathVariable Long orderId) {
        return ApiResponse.ok(queries.order(orderId));
    }

    @GetMapping("/branches")
    public ApiResponse<List<CatalogDtos.BranchResponse>> branches() {
        return ApiResponse.ok(catalog.branches());
    }

    @PostMapping("/branches")
    public ResponseEntity<ApiResponse<CatalogDtos.BranchResponse>> createBranch(
            @Valid @RequestBody AdminDtos.BranchWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalog.createBranch(request)));
    }

    @PutMapping("/branches/{id}")
    public ApiResponse<CatalogDtos.BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.BranchWriteRequest request
    ) {
        return ApiResponse.ok(catalog.updateBranch(id, request));
    }

    @PatchMapping("/branches/{id}/operations")
    public ApiResponse<CatalogDtos.BranchResponse> operations(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.BranchOperationsRequest request
    ) {
        return ApiResponse.ok(catalog.updateOperations(id, request));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CatalogDtos.CategoryResponse>> categories() {
        return ApiResponse.ok(catalog.categories());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CatalogDtos.CategoryResponse>> createCategory(
            @Valid @RequestBody AdminDtos.CategoryWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalog.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CatalogDtos.CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.CategoryWriteRequest request
    ) {
        return ApiResponse.ok(catalog.updateCategory(id, request));
    }

    @GetMapping("/menu")
    public ApiResponse<AdminDtos.AdminMenuResponse> menu() {
        return ApiResponse.ok(catalog.menu());
    }

    @PostMapping("/menu/items")
    public ResponseEntity<ApiResponse<AdminDtos.AdminMenuItem>> createItem(
            @Valid @RequestBody AdminDtos.MenuItemWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalog.createItem(request)));
    }

    @PutMapping("/menu/items/{id}")
    public ApiResponse<AdminDtos.AdminMenuItem> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.MenuItemWriteRequest request
    ) {
        return ApiResponse.ok(catalog.updateItem(id, request));
    }

    @DeleteMapping("/menu/items/{id}")
    public ApiResponse<Void> deactivateItem(@PathVariable Long id) {
        catalog.deactivateItem(id);
        return ApiResponse.message(null, "Menu item deactivated.");
    }

    @PutMapping("/branches/{branchId}/menu/{itemId}")
    public ApiResponse<AdminDtos.BranchAvailability> availability(
            @PathVariable Long branchId,
            @PathVariable Long itemId,
            @Valid @RequestBody AdminDtos.AvailabilityRequest request
    ) {
        return ApiResponse.ok(catalog.setAvailability(branchId, itemId, request.available()));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<AuthDtos.UserResponse>> staff(@Valid @RequestBody AdminDtos.StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalog.createKitchenStaff(request)));
    }
}
