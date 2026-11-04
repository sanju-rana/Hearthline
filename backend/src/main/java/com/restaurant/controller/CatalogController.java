package com.restaurant.controller;

import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.CatalogDtos;
import com.restaurant.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/restaurants")
    public ApiResponse<List<CatalogDtos.RestaurantResponse>> restaurants() {
        return ApiResponse.ok(catalogService.restaurants());
    }

    @GetMapping("/restaurants/{id}")
    public ApiResponse<CatalogDtos.RestaurantResponse> restaurant(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.restaurant(id));
    }

    @GetMapping("/branches")
    public ApiResponse<List<CatalogDtos.BranchResponse>> branches() {
        return ApiResponse.ok(catalogService.branches());
    }

    @GetMapping("/branches/{id}")
    public ApiResponse<CatalogDtos.BranchResponse> branch(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.branch(id));
    }

    @GetMapping("/branches/{branchId}/menu")
    public ApiResponse<List<CatalogDtos.MenuItemResponse>> menu(
            @PathVariable Long branchId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "false") boolean popularOnly
    ) {
        return ApiResponse.ok(catalogService.menu(branchId, categoryId, search, sort, availableOnly, popularOnly));
    }

    @GetMapping("/menu/categories")
    public ApiResponse<List<CatalogDtos.CategoryResponse>> categories() {
        return ApiResponse.ok(catalogService.categories());
    }

    @GetMapping("/menu/featured")
    public ApiResponse<List<CatalogDtos.MenuItemResponse>> featured() {
        return ApiResponse.ok(catalogService.featured());
    }

    @GetMapping("/menu/items/{id}")
    public ApiResponse<CatalogDtos.MenuItemResponse> item(
            @PathVariable Long id,
            @RequestParam Long branchId
    ) {
        return ApiResponse.ok(catalogService.item(id, branchId));
    }
}
