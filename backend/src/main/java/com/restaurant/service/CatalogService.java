package com.restaurant.service;

import com.restaurant.dto.CatalogDtos;
import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.MenuCategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.RestaurantBranchRepository;
import com.restaurant.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CatalogService {

    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final BranchMenuItemRepository offerings;

    public CatalogService(
            RestaurantRepository restaurants,
            RestaurantBranchRepository branches,
            MenuCategoryRepository categories,
            MenuItemRepository menuItems,
            BranchMenuItemRepository offerings
    ) {
        this.restaurants = restaurants;
        this.branches = branches;
        this.categories = categories;
        this.menuItems = menuItems;
        this.offerings = offerings;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.RestaurantResponse> restaurants() {
        return restaurants.findByActiveTrueOrderByNameAsc().stream().map(ViewMapper::toRestaurant).toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.RestaurantResponse restaurant(Long id) {
        return restaurants.findById(id)
                .filter(restaurant -> restaurant.isActive())
                .map(ViewMapper::toRestaurant)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found."));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BranchResponse> branches() {
        return branches.findActiveWithRestaurant().stream().map(ViewMapper::toBranch).toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.BranchResponse branch(Long id) {
        return ViewMapper.toBranch(activeBranch(id));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryResponse> categories() {
        return categories.findByActiveTrueOrderByDisplayOrderAsc().stream().map(ViewMapper::toCategory).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MenuItemResponse> featured() {
        return menuItems.findFeatured().stream()
                .map(item -> ViewMapper.toMenuItem(item, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MenuItemResponse> menu(
            Long branchId,
            Long categoryId,
            String search,
            String sort,
            boolean availableOnly,
            boolean popularOnly
    ) {
        activeBranch(branchId);
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Comparator<BranchMenuItem> comparator = comparator(sort);
        return offerings.findCatalog(branchId).stream()
                .filter(offering -> categoryId == null || offering.getMenuItem().getCategory().getId().equals(categoryId))
                .filter(offering -> !availableOnly || offering.isAvailable())
                .filter(offering -> !popularOnly || offering.getMenuItem().isPopular())
                .filter(offering -> matches(offering.getMenuItem(), needle))
                .sorted(comparator)
                .map(offering -> ViewMapper.toMenuItem(offering.getMenuItem(), offering.isAvailable()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.MenuItemResponse item(Long itemId, Long branchId) {
        activeBranch(branchId);
        BranchMenuItem offering = offerings.findOffering(branchId, itemId)
                .filter(found -> found.getMenuItem().isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        return ViewMapper.toMenuItem(offering.getMenuItem(), offering.isAvailable());
    }

    private RestaurantBranch activeBranch(Long id) {
        RestaurantBranch branch = branches.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant branch not found."));
        if (!branch.isActive()) {
            throw new ResourceNotFoundException("Restaurant branch not found.");
        }
        return branch;
    }

    private boolean matches(MenuItem item, String needle) {
        if (needle.isBlank()) {
            return true;
        }
        return item.getName().toLowerCase(Locale.ROOT).contains(needle)
                || item.getDescription().toLowerCase(Locale.ROOT).contains(needle)
                || item.getCategory().getName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private Comparator<BranchMenuItem> comparator(String sort) {
        if ("priceAsc".equals(sort)) {
            return Comparator.comparing(offering -> offering.getMenuItem().getPrice());
        }
        if ("priceDesc".equals(sort)) {
            return Comparator.comparing((BranchMenuItem offering) -> offering.getMenuItem().getPrice()).reversed();
        }
        return Comparator.comparing(offering -> offering.getMenuItem().getName());
    }
}
