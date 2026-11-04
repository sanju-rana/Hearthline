package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.dto.OrderDtos;
import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.Cart;
import com.restaurant.entity.CartItem;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.MenuCategory;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.PaymentOutcome;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.PaymentDeclinedException;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.CartRepository;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.websocket.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CustomerOrderRepository orders;
    @Mock private CartRepository carts;
    @Mock private BranchMenuItemRepository offerings;
    @Mock private UserAccountRepository users;
    @Mock private PaymentRepository payments;
    @Mock private PaymentGateway paymentGateway;
    @Mock private OrderEventPublisher events;

    private OrderService service;
    private RestaurantBranch branch;
    private MenuItem menuItem;
    private Cart cart;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("test-only-hearthline-jwt-secret-32-characters", 30, "hearthline_token", false),
                new AppProperties.Payment("demo", true),
                new AppProperties.Order(new BigDecimal("0.05"), new BigDecimal("40.00"), "INR", 15, 2, 45, "Asia/Kolkata"),
                new AppProperties.Cors(List.of("http://localhost:5173")));
        service = new OrderService(orders, carts, offerings, users, payments, paymentGateway, properties, events);

        branch = new RestaurantBranch();
        branch.setId(1L);
        branch.setName("Hearthline Indiranagar");
        branch.setActive(true);
        branch.setAcceptingOrders(true);

        MenuCategory category = new MenuCategory();
        category.setId(1L);
        category.setName("Pizza");
        menuItem = new MenuItem();
        menuItem.setId(8L);
        menuItem.setCategory(category);
        menuItem.setName("Margherita");
        menuItem.setPrice(new BigDecimal("399.00"));
        menuItem.setVegetarian(true);
        menuItem.setActive(true);

        CartItem line = new CartItem();
        line.setMenuItem(menuItem);
        line.setQuantity(1);
        cart = new Cart();
        cart.setBranch(branch);
        cart.addItem(line);
    }

    @Test
    void orderTotalComesFromTheDatabasePrice() {
        stubNewOrder();
        when(paymentGateway.charge(new BigDecimal("418.95"), PaymentOutcome.SUCCESS))
                .thenReturn(new PaymentGateway.PaymentCharge(PaymentStatus.SUCCESS, "DEMO-1", null));

        OrderDtos.OrderDetailResponse response = service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001");

        assertEquals(new BigDecimal("399.00"), response.subtotal());
        assertEquals(new BigDecimal("19.95"), response.taxAmount());
        assertEquals(new BigDecimal("0.00"), response.deliveryFee());
        assertEquals(new BigDecimal("418.95"), response.totalAmount());
        assertEquals("DEMO", response.payment().provider());
        assertTrue(cart.getItems().isEmpty());
        verify(events).publishAfterCommit(any(), org.mockito.ArgumentMatchers.eq("ORDER_CREATED"));
    }

    @Test
    void unavailableItemIsRejected() {
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));
        BranchMenuItem hidden = offering();
        hidden.setAvailable(false);
        when(offerings.findByBranchIdAndMenuItemId(1L, 8L)).thenReturn(Optional.of(hidden));

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001"));
        assertEquals("This menu item is currently unavailable.", error.getMessage());
        verify(orders, never()).save(any());
    }

    @Test
    void itemFromAnotherBranchIsRejected() {
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));
        when(offerings.findByBranchIdAndMenuItemId(1L, 8L)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001"));
        verify(orders, never()).save(any());
    }

    @Test
    void invalidQuantityIsRejected() {
        cart.getItems().get(0).setQuantity(0);
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));

        assertThrows(ConflictException.class, () -> service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001"));
        verify(paymentGateway, never()).charge(any(), any());
    }

    @Test
    void emptyCartIsRejected() {
        cart.getItems().clear();
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));

        assertThrows(ConflictException.class, () -> service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001"));
    }

    @Test
    void declinedPaymentDoesNotSaveAnOrder() {
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));
        when(offerings.findByBranchIdAndMenuItemId(1L, 8L)).thenReturn(Optional.of(offering()));
        when(paymentGateway.charge(any(), org.mockito.ArgumentMatchers.eq(PaymentOutcome.FAILURE)))
                .thenReturn(new PaymentGateway.PaymentCharge(PaymentStatus.FAILED, "DEMO-2", "declined"));

        assertThrows(PaymentDeclinedException.class,
                () -> service.place(4L, request(PaymentOutcome.FAILURE), "checkout-key-001"));
        verify(orders, never()).save(any());
    }

    @Test
    void pendingPaymentDoesNotSaveAnOrder() {
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));
        when(offerings.findByBranchIdAndMenuItemId(1L, 8L)).thenReturn(Optional.of(offering()));
        when(paymentGateway.charge(any(), org.mockito.ArgumentMatchers.eq(PaymentOutcome.PENDING)))
                .thenReturn(new PaymentGateway.PaymentCharge(PaymentStatus.PENDING, "DEMO-3", "pending"));

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.place(4L, request(PaymentOutcome.PENDING), "checkout-key-001"));
        assertEquals("Payment is still pending.", error.getMessage());
        verify(orders, never()).save(any());
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() {
        CustomerOrder existing = new CustomerOrder();
        existing.setId(9L);
        existing.setOrderNumber("HL-1001");
        existing.setBranch(branch);
        existing.setOrderType(OrderType.PICKUP);
        existing.setStatus(com.restaurant.enums.OrderStatus.PLACED);
        existing.setCustomerName("Asha Raman");
        existing.setCustomerPhone("9845001122");
        existing.setSubtotal(new BigDecimal("399.00"));
        existing.setTaxAmount(new BigDecimal("19.95"));
        existing.setDeliveryFee(new BigDecimal("0.00"));
        existing.setTotalAmount(new BigDecimal("418.95"));
        existing.setCurrency("INR");
        existing.setEstimatedPrepMinutes(17);
        existing.setIdempotencyFingerprint(Idempotency.fingerprint(request(PaymentOutcome.SUCCESS)));
        UserAccount customer = new UserAccount();
        customer.setId(4L);
        existing.setCustomer(customer);
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.of(existing));

        OrderDtos.OrderDetailResponse response = service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001");

        assertEquals("HL-1001", response.orderNumber());
        verify(paymentGateway, never()).charge(any(), any());
        verify(orders, never()).save(any());
    }

    @Test
    void sameKeyWithADifferentRequestConflicts() {
        CustomerOrder existing = new CustomerOrder();
        existing.setIdempotencyFingerprint("different");
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.place(4L, request(PaymentOutcome.SUCCESS), "checkout-key-001"));
    }

    private void stubNewOrder() {
        when(orders.findByCustomerIdAndIdempotencyKey(4L, "checkout-key-001")).thenReturn(Optional.empty());
        when(carts.lockByCustomerId(4L)).thenReturn(Optional.of(cart));
        when(offerings.findByBranchIdAndMenuItemId(1L, 8L)).thenReturn(Optional.of(offering()));
        UserAccount customer = new UserAccount();
        customer.setId(4L);
        when(users.findById(4L)).thenReturn(Optional.of(customer));
        when(orders.nextOrderNumber()).thenReturn(1001L);
        when(orders.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private BranchMenuItem offering() {
        BranchMenuItem offering = new BranchMenuItem();
        offering.setBranch(branch);
        offering.setMenuItem(menuItem);
        offering.setAvailable(true);
        return offering;
    }

    private OrderDtos.PlaceOrderRequest request(PaymentOutcome outcome) {
        return new OrderDtos.PlaceOrderRequest(OrderType.PICKUP, "Asha Raman", "9845001122", null, "", outcome);
    }
}
