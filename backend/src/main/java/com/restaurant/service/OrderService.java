package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.dto.OrderDtos;
import com.restaurant.dto.PageResponse;
import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.Cart;
import com.restaurant.entity.CartItem;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.OrderItem;
import com.restaurant.entity.OrderStatusHistory;
import com.restaurant.entity.Payment;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.ForbiddenOperationException;
import com.restaurant.exception.PaymentDeclinedException;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.CartRepository;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.websocket.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Places an order from the saved cart.
 * Prices are read from the database. The browser cannot submit its own totals.
 * The kitchen is notified only after this transaction commits.
 */
@Service
public class OrderService {

    private final CustomerOrderRepository orders;
    private final CartRepository carts;
    private final BranchMenuItemRepository offerings;
    private final UserAccountRepository users;
    private final PaymentRepository payments;
    private final PaymentGateway paymentGateway;
    private final AppProperties properties;
    private final OrderEventPublisher events;

    public OrderService(
            CustomerOrderRepository orders,
            CartRepository carts,
            BranchMenuItemRepository offerings,
            UserAccountRepository users,
            PaymentRepository payments,
            PaymentGateway paymentGateway,
            AppProperties properties,
            OrderEventPublisher events
    ) {
        this.orders = orders;
        this.carts = carts;
        this.offerings = offerings;
        this.users = users;
        this.payments = payments;
        this.paymentGateway = paymentGateway;
        this.properties = properties;
        this.events = events;
    }

    @Transactional
    public OrderDtos.OrderDetailResponse place(Long customerId, OrderDtos.PlaceOrderRequest request, String idempotencyKey) {
        Idempotency.checkKey(idempotencyKey);
        String fingerprint = Idempotency.fingerprint(request);
        CustomerOrder existing = orders.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getIdempotencyFingerprint().equals(fingerprint)) {
                throw new ConflictException("This Idempotency-Key was already used for a different order.");
            }
            return ViewMapper.toDetail(initialize(existing));
        }

        if (request.orderType() == OrderType.DELIVERY
                && (request.deliveryAddress() == null || request.deliveryAddress().isBlank())) {
            throw new com.restaurant.exception.BadRequestException("A delivery address is required.");
        }

        Cart cart = carts.lockByCustomerId(customerId)
                .orElseThrow(() -> new ConflictException("Your cart is empty."));
        if (cart.getBranch() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("Your cart is empty.");
        }
        RestaurantBranch branch = cart.getBranch();
        if (!branch.isActive() || !branch.isAcceptingOrders()) {
            throw new ConflictException("This restaurant is not accepting orders right now.");
        }

        List<PricedLine> lines = priceFromDatabase(branch, cart.getItems());
        BigDecimal subtotal = lines.stream().map(PricedLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = OrderPricing.tax(subtotal, properties.order().taxRate());
        BigDecimal deliveryFee = OrderPricing.deliveryFee(request.orderType(), properties.order().deliveryFee());
        BigDecimal total = OrderPricing.total(subtotal, tax, deliveryFee);
        int quantitySum = lines.stream().mapToInt(line -> line.quantity).sum();

        PaymentGateway.PaymentCharge charge = paymentGateway.charge(total, request.paymentOutcome());
        if (charge.status() == PaymentStatus.FAILED) {
            throw new PaymentDeclinedException("Payment was declined.");
        }
        if (charge.status() == PaymentStatus.PENDING) {
            throw new ConflictException("Payment is still pending.");
        }

        UserAccount customer = users.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        Instant now = Instant.now();
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("HL-" + orders.nextOrderNumber());
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setOrderType(request.orderType());
        order.setStatus(OrderStatus.PLACED);
        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.phone());
        order.setDeliveryAddress(request.orderType() == OrderType.DELIVERY ? request.deliveryAddress().trim() : null);
        order.setSpecialInstructions(blankToNull(request.specialInstructions()));
        order.setSubtotal(OrderPricing.money(subtotal));
        order.setTaxAmount(tax);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(total);
        order.setCurrency(properties.order().currency());
        order.setEstimatedPrepMinutes(PrepTimeEstimator.estimate(
                quantitySum,
                properties.order().prepBaseMinutes(),
                properties.order().prepPerItemMinutes(),
                properties.order().prepMaxMinutes()));
        order.setIdempotencyKey(idempotencyKey);
        order.setIdempotencyFingerprint(fingerprint);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        for (PricedLine line : lines) {
            order.addItem(line.toEntity());
        }
        order.attachPayment(payment(order, charge, total, now));
        order.addHistory(history(order, customer, null, OrderStatus.PLACED, "Order placed", now));
        orders.save(order);
        cart.getItems().clear();
        events.publishAfterCommit(order, "ORDER_CREATED");
        return ViewMapper.toDetail(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderSummaryResponse> myOrders(Long customerId, int page, int size) {
        var result = orders.findByCustomerIdOrderByCreatedAtDesc(customerId, Pages.of(page, size));
        Map<Long, Payment> paymentByOrder = payments.findByOrderIdIn(
                result.getContent().stream().map(CustomerOrder::getId).toList()).stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), Function.identity()));
        List<OrderDtos.OrderSummaryResponse> content = result.getContent().stream()
                .map(order -> ViewMapper.toSummary(order, paymentByOrder.get(order.getId())))
                .toList();
        return Pages.wrap(result, content);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderDetailResponse getOwn(Long customerId, Long orderId) {
        CustomerOrder order = initialize(find(orderId));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenOperationException("You are not authorized to access this order.");
        }
        return ViewMapper.toDetail(order);
    }

    @Transactional
    public OrderDtos.OrderDetailResponse cancel(Long customerId, Long orderId) {
        CustomerOrder order = initialize(find(orderId));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenOperationException("You are not authorized to access this order.");
        }
        OrderStatusMachine.assertCustomerCancel(order.getStatus());
        UserAccount customer = users.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        move(order, customer, OrderStatus.CANCELLED, "Cancelled by customer");
        cancelDemoPayment(order);
        events.publishAfterCommit(order, "ORDER_UPDATED");
        return ViewMapper.toDetail(order);
    }

    private List<PricedLine> priceFromDatabase(RestaurantBranch branch, List<CartItem> items) {
        List<PricedLine> lines = new ArrayList<>();
        for (CartItem cartItem : items) {
            if (cartItem.getQuantity() < 1 || cartItem.getQuantity() > 20) {
                throw new ConflictException("Quantity must be between 1 and 20.");
            }
            MenuItem menuItem = cartItem.getMenuItem();
            BranchMenuItem offering = offerings.findByBranchIdAndMenuItemId(branch.getId(), menuItem.getId())
                    .orElseThrow(() -> new ConflictException("This menu item is currently unavailable."));
            if (!menuItem.isActive() || !offering.isAvailable()) {
                throw new ConflictException("This menu item is currently unavailable.");
            }
            lines.add(new PricedLine(
                    menuItem,
                    menuItem.getName(),
                    menuItem.getPrice(),
                    cartItem.getQuantity(),
                    OrderPricing.lineTotal(menuItem.getPrice(), cartItem.getQuantity()),
                    menuItem.isVegetarian()));
        }
        return lines;
    }

    private Payment payment(CustomerOrder order, PaymentGateway.PaymentCharge charge, BigDecimal total, Instant now) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(charge.status());
        payment.setAmount(total);
        payment.setCurrency(properties.order().currency());
        payment.setProvider("DEMO");
        payment.setProviderReference(charge.reference());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return payment;
    }

    private void cancelDemoPayment(CustomerOrder order) {
        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason("Demo payment marked cancelled. No money was moved.");
        }
    }

    private void move(CustomerOrder order, UserAccount actor, OrderStatus to, String note) {
        OrderStatus from = order.getStatus();
        order.setStatus(to);
        order.setUpdatedAt(Instant.now());
        order.addHistory(history(order, actor, from, to, note, order.getUpdatedAt()));
    }

    private OrderStatusHistory history(
            CustomerOrder order,
            UserAccount actor,
            OrderStatus from,
            OrderStatus to,
            String note,
            Instant when
    ) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrder(order);
        entry.setChangedBy(actor);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setNote(note);
        entry.setCreatedAt(when);
        return entry;
    }

    private CustomerOrder find(Long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private CustomerOrder initialize(CustomerOrder order) {
        order.getItems().size();
        order.getHistory().size();
        order.getBranch().getName();
        order.getCustomer().getId();
        if (order.getPayment() != null) {
            order.getPayment().getStatus();
        }
        return order;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PricedLine(
            MenuItem menuItem,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            boolean vegetarian
    ) {
        OrderItem toEntity() {
            OrderItem item = new OrderItem();
            item.setMenuItem(menuItem);
            item.setItemName(name);
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setLineTotal(lineTotal);
            item.setVegetarian(vegetarian);
            return item;
        }
    }
}
