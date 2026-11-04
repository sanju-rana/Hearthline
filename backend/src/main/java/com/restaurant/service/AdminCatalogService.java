package com.restaurant.service;

import com.restaurant.dto.AdminDtos;
import com.restaurant.dto.AuthDtos;
import com.restaurant.dto.CatalogDtos;
import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.MenuCategory;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.UserRole;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.MenuCategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.RestaurantBranchRepository;
import com.restaurant.repository.RestaurantRepository;
import com.restaurant.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCatalogService {

    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final BranchMenuItemRepository offerings;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminCatalogService(
            RestaurantRepository restaurants,
            RestaurantBranchRepository branches,
            MenuCategoryRepository categories,
            MenuItemRepository menuItems,
            BranchMenuItemRepository offerings,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder
    ) {
        this.restaurants = restaurants;
        this.branches = branches;
        this.categories = categories;
        this.menuItems = menuItems;
        this.offerings = offerings;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BranchResponse> branches() {
        return branches.findAllWithRestaurant().stream().map(ViewMapper::toBranch).toList();
    }

    @Transactional
    public CatalogDtos.BranchResponse createBranch(AdminDtos.BranchWriteRequest request) {
        Restaurant restaurant = restaurants.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found."));
        RestaurantBranch branch = new RestaurantBranch();
        apply(branch, restaurant, request);
        branches.save(branch);
        for (MenuItem item : menuItems.findAll()) {
            BranchMenuItem offering = new BranchMenuItem();
            offering.setBranch(branch);
            offering.setMenuItem(item);
            offering.setAvailable(item.isActive());
            offerings.save(offering);
        }
        return ViewMapper.toBranch(branch);
    }

    @Transactional
    public CatalogDtos.BranchResponse updateBranch(Long id, AdminDtos.BranchWriteRequest request) {
        RestaurantBranch branch = findBranch(id);
        Restaurant restaurant = restaurants.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found."));
        apply(branch, restaurant, request);
        return ViewMapper.toBranch(branch);
    }

    @Transactional
    public CatalogDtos.BranchResponse updateOperations(Long id, AdminDtos.BranchOperationsRequest request) {
        RestaurantBranch branch = findBranch(id);
        branch.setActive(request.active());
        branch.setAcceptingOrders(request.acceptingOrders());
        return ViewMapper.toBranch(branch);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryResponse> categories() {
        return categories.findAllByOrderByDisplayOrderAsc().stream().map(ViewMapper::toCategory).toList();
    }

    @Transactional
    public CatalogDtos.CategoryResponse createCategory(AdminDtos.CategoryWriteRequest request) {
        if (categories.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new ConflictException("A category with this name already exists.");
        }
        MenuCategory category = new MenuCategory();
        apply(category, request);
        categories.save(category);
        return ViewMapper.toCategory(category);
    }

    @Transactional
    public CatalogDtos.CategoryResponse updateCategory(Long id, AdminDtos.CategoryWriteRequest request) {
        MenuCategory category = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        categories.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A category with this name already exists.");
                });
        apply(category, request);
        return ViewMapper.toCategory(category);
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminMenuResponse menu() {
        List<RestaurantBranch> branchList = branches.findAllWithRestaurant();
        List<AdminDtos.AdminMenuItem> items = menuItems.findAllWithCategory().stream()
                .map(item -> toAdminItem(item, branchList))
                .toList();
        return new AdminDtos.AdminMenuResponse(
                categories.findAllByOrderByDisplayOrderAsc().stream().map(ViewMapper::toCategory).toList(),
                items,
                branchList.stream().map(ViewMapper::toBranch).toList());
    }

    @Transactional
    public AdminDtos.AdminMenuItem createItem(AdminDtos.MenuItemWriteRequest request) {
        MenuItem item = new MenuItem();
        apply(item, request);
        menuItems.save(item);
        List<RestaurantBranch> branchList = branches.findAll();
        for (RestaurantBranch branch : branchList) {
            BranchMenuItem offering = new BranchMenuItem();
            offering.setBranch(branch);
            offering.setMenuItem(item);
            offering.setAvailable(true);
            offerings.save(offering);
        }
        return toAdminItem(item, branches.findAllWithRestaurant());
    }

    @Transactional
    public AdminDtos.AdminMenuItem updateItem(Long id, AdminDtos.MenuItemWriteRequest request) {
        MenuItem item = menuItems.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        apply(item, request);
        return toAdminItem(item, branches.findAllWithRestaurant());
    }

    @Transactional
    public void deactivateItem(Long id) {
        MenuItem item = menuItems.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        item.setActive(false);
    }

    @Transactional
    public AdminDtos.BranchAvailability setAvailability(Long branchId, Long itemId, boolean available) {
        RestaurantBranch branch = findBranch(branchId);
        MenuItem item = menuItems.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        BranchMenuItem offering = offerings.findByBranchIdAndMenuItemId(branchId, itemId).orElseGet(() -> {
            BranchMenuItem created = new BranchMenuItem();
            created.setBranch(branch);
            created.setMenuItem(item);
            return created;
        });
        offering.setAvailable(available);
        offerings.save(offering);
        return new AdminDtos.BranchAvailability(branch.getId(), branch.getName(), offering.isAvailable());
    }

    @Transactional
    public AuthDtos.UserResponse createKitchenStaff(AdminDtos.StaffRequest request) {
        PasswordRules.check(request.password());
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        RestaurantBranch branch = findBranch(request.branchId());
        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.KITCHEN_STAFF);
        user.setBranch(branch);
        user.setEnabled(true);
        user.setTokenVersion(0);
        users.save(user);
        return ViewMapper.toUser(user);
    }

    private AdminDtos.AdminMenuItem toAdminItem(MenuItem item, List<RestaurantBranch> branchList) {
        List<BranchMenuItem> rows = offerings.findByMenuItemId(item.getId());
        List<AdminDtos.BranchAvailability> availability = branchList.stream()
                .map(branch -> {
                    boolean available = rows.stream()
                            .filter(row -> row.getBranch().getId().equals(branch.getId()))
                            .map(BranchMenuItem::isAvailable)
                            .findFirst()
                            .orElse(false);
                    return new AdminDtos.BranchAvailability(branch.getId(), branch.getName(), available);
                })
                .toList();
        return new AdminDtos.AdminMenuItem(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isVegetarian(),
                item.isPopular(),
                item.getImageUrl(),
                item.isActive(),
                availability);
    }

    private void apply(RestaurantBranch branch, Restaurant restaurant, AdminDtos.BranchWriteRequest request) {
        branch.setRestaurant(restaurant);
        branch.setName(request.name().trim());
        branch.setAddressLine(request.addressLine().trim());
        branch.setCity(request.city().trim());
        branch.setState(request.state().trim());
        branch.setPostalCode(request.postalCode().trim());
        branch.setPhone(request.phone());
        branch.setOpensAt(request.opensAt());
        branch.setClosesAt(request.closesAt());
        branch.setActive(request.active());
        branch.setAcceptingOrders(request.acceptingOrders());
    }

    private void apply(MenuCategory category, AdminDtos.CategoryWriteRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description() == null ? null : request.description().trim());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.active());
    }

    private void apply(MenuItem item, AdminDtos.MenuItemWriteRequest request) {
        MenuCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        item.setCategory(category);
        item.setName(request.name().trim());
        item.setDescription(request.description().trim());
        item.setPrice(OrderPricing.money(request.price()));
        item.setVegetarian(request.vegetarian());
        item.setPopular(request.popular());
        item.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        item.setActive(request.active());
    }

    private RestaurantBranch findBranch(Long id) {
        return branches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Restaurant branch not found."));
    }
}
