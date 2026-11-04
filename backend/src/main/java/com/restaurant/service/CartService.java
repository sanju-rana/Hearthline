package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.dto.CartDtos;
import com.restaurant.dto.CatalogDtos;
import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.Cart;
import com.restaurant.entity.CartItem;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderType;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.CartRepository;
import com.restaurant.repository.RestaurantBranchRepository;
import com.restaurant.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository carts;
    private final UserAccountRepository users;
    private final RestaurantBranchRepository branches;
    private final BranchMenuItemRepository offerings;
    private final AppProperties properties;

    public CartService(
            CartRepository carts,
            UserAccountRepository users,
            RestaurantBranchRepository branches,
            BranchMenuItemRepository offerings,
            AppProperties properties
    ) {
        this.carts = carts;
        this.users = users;
        this.branches = branches;
        this.offerings = offerings;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public CartDtos.CartResponse get(Long customerId) {
        return toResponse(carts.findDetailedByCustomerId(customerId).orElse(null));
    }

    @Transactional
    public CartDtos.CartResponse setBranch(Long customerId, Long branchId) {
        RestaurantBranch branch = openBranch(branchId);
        Cart cart = loadOrCreate(customerId);
        if (cart.getBranch() != null && !cart.getBranch().getId().equals(branch.getId())) {
            cart.getItems().clear();
        }
        cart.setBranch(branch);
        return toResponse(carts.findDetailedByCustomerId(customerId).orElse(cart));
    }

    @Transactional
    public CartDtos.CartResponse addItem(Long customerId, CartDtos.AddCartItemRequest request) {
        Cart cart = loadOrCreate(customerId);
        if (cart.getBranch() == null) {
            throw new ConflictException("Choose a restaurant branch before adding items.");
        }
        BranchMenuItem offering = requireAvailable(cart.getBranch().getId(), request.menuItemId());
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(request.menuItemId()))
                .findFirst()
                .orElse(null);
        int nextQuantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (nextQuantity > 20) {
            throw new ConflictException("Quantity cannot exceed 20.");
        }
        if (existing == null) {
            CartItem item = new CartItem();
            item.setMenuItem(offering.getMenuItem());
            item.setQuantity(request.quantity());
            cart.addItem(item);
        } else {
            existing.setQuantity(nextQuantity);
        }
        carts.save(cart);
        return toResponse(carts.findDetailedByCustomerId(customerId).orElse(cart));
    }

    @Transactional
    public CartDtos.CartResponse updateItem(Long customerId, Long cartItemId, int quantity) {
        Cart cart = requireCart(customerId);
        CartItem item = findLine(cart, cartItemId);
        requireAvailable(cart.getBranch().getId(), item.getMenuItem().getId());
        item.setQuantity(quantity);
        return toResponse(carts.findDetailedByCustomerId(customerId).orElse(cart));
    }

    @Transactional
    public CartDtos.CartResponse removeItem(Long customerId, Long cartItemId) {
        Cart cart = requireCart(customerId);
        CartItem item = findLine(cart, cartItemId);
        cart.getItems().remove(item);
        return toResponse(carts.findDetailedByCustomerId(customerId).orElse(cart));
    }

    @Transactional
    public CartDtos.CartResponse clear(Long customerId) {
        Cart cart = carts.findDetailedByCustomerId(customerId).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cart.setBranch(null);
        }
        return toResponse(cart);
    }

    private Cart requireCart(Long customerId) {
        Cart cart = carts.findDetailedByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));
        if (cart.getBranch() == null) {
            throw new ConflictException("Choose a restaurant branch before changing the cart.");
        }
        return cart;
    }

    private CartItem findLine(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));
    }

    private BranchMenuItem requireAvailable(Long branchId, Long menuItemId) {
        BranchMenuItem offering = offerings.findOffering(branchId, menuItemId)
                .orElseThrow(() -> new ConflictException("This menu item is currently unavailable."));
        if (!offering.getMenuItem().isActive() || !offering.isAvailable()) {
            throw new ConflictException("This menu item is currently unavailable.");
        }
        return offering;
    }

    private RestaurantBranch openBranch(Long branchId) {
        RestaurantBranch branch = branches.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant branch not found."));
        if (!branch.isActive()) {
            throw new ConflictException("This restaurant is temporarily unavailable.");
        }
        return branch;
    }

    private Cart loadOrCreate(Long customerId) {
        return carts.findByCustomerId(customerId).orElseGet(() -> {
            UserAccount customer = users.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
            Cart cart = new Cart();
            cart.setCustomer(customer);
            return carts.save(cart);
        });
    }

    private CartDtos.CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return empty(null);
        }
        List<CartDtos.CartLineResponse> lines = cart.getItems().stream().map(this::toLine).toList();
        BigDecimal subtotal = lines.stream()
                .map(CartDtos.CartLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = OrderPricing.tax(subtotal, properties.order().taxRate());
        BigDecimal deliveryFee = OrderPricing.deliveryFee(OrderType.DELIVERY, properties.order().deliveryFee());
        int count = lines.stream().mapToInt(CartDtos.CartLineResponse::quantity).sum();
        CatalogDtos.BranchResponse branch = cart.getBranch() == null ? null : ViewMapper.toBranch(cart.getBranch());
        return new CartDtos.CartResponse(
                branch,
                lines,
                OrderPricing.money(subtotal),
                tax,
                deliveryFee,
                OrderPricing.total(subtotal, tax, OrderPricing.money(BigDecimal.ZERO)),
                OrderPricing.total(subtotal, tax, deliveryFee),
                properties.order().currency(),
                count);
    }

    private CartDtos.CartLineResponse toLine(CartItem item) {
        MenuItem menuItem = item.getMenuItem();
        boolean available = menuItem.isActive();
        if (item.getCart().getBranch() != null) {
            available = offerings.findOffering(item.getCart().getBranch().getId(), menuItem.getId())
                    .map(offering -> offering.isAvailable() && menuItem.isActive())
                    .orElse(false);
        }
        BigDecimal lineTotal = OrderPricing.lineTotal(menuItem.getPrice(), item.getQuantity());
        return new CartDtos.CartLineResponse(
                item.getId(),
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getImageUrl(),
                menuItem.isVegetarian(),
                available,
                menuItem.getPrice(),
                item.getQuantity(),
                lineTotal);
    }

    private CartDtos.CartResponse empty(CatalogDtos.BranchResponse branch) {
        BigDecimal zero = new BigDecimal("0.00");
        BigDecimal deliveryFee = OrderPricing.deliveryFee(OrderType.DELIVERY, properties.order().deliveryFee());
        return new CartDtos.CartResponse(
                branch, List.of(), zero, zero, deliveryFee, zero, deliveryFee, properties.order().currency(), 0);
    }
}
