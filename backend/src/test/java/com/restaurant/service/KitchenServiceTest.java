package com.restaurant.service;

import com.restaurant.dto.OrderDtos;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.UserRole;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.ForbiddenOperationException;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.websocket.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock private CustomerOrderRepository orders;
    @Mock private UserAccountRepository users;
    @Mock private OrderEventPublisher events;

    private KitchenService service;
    private RestaurantBranch branch;
    private RestaurantUserDetails staff;

    @BeforeEach
    void setUp() {
        service = new KitchenService(orders, users, events);
        branch = new RestaurantBranch();
        branch.setId(1L);
        branch.setName("Hearthline Indiranagar");
        UserAccount account = new UserAccount();
        account.setId(5L);
        account.setEmail("kitchen.indiranagar@hearthline.test");
        account.setPasswordHash("hash");
        account.setRole(UserRole.KITCHEN_STAFF);
        account.setBranch(branch);
        account.setEnabled(true);
        staff = new RestaurantUserDetails(account);
    }

    @Test
    void staffCanAcceptAnOrderForTheirBranch() {
        CustomerOrder order = placedOrder(branch);
        when(orders.findById(9L)).thenReturn(Optional.of(order));
        UserAccount actor = new UserAccount();
        actor.setId(5L);
        when(users.findById(5L)).thenReturn(Optional.of(actor));

        OrderDtos.KitchenOrderResponse response = service.updateStatus(
                staff, 9L, new OrderDtos.StatusChangeRequest(OrderStatus.ACCEPTED, null));

        assertEquals(OrderStatus.ACCEPTED, response.status());
        verify(events).publishAfterCommit(order, "ORDER_UPDATED");
    }

    @Test
    void illegalTransitionIsRejected() {
        when(orders.findById(9L)).thenReturn(Optional.of(placedOrder(branch)));
        assertThrows(ConflictException.class, () -> service.updateStatus(
                staff, 9L, new OrderDtos.StatusChangeRequest(OrderStatus.COMPLETED, null)));
    }

    @Test
    void staffCannotUpdateAnotherBranch() {
        RestaurantBranch other = new RestaurantBranch();
        other.setId(2L);
        other.setName("Hearthline Bandra");
        when(orders.findById(9L)).thenReturn(Optional.of(placedOrder(other)));

        assertThrows(ForbiddenOperationException.class, () -> service.updateStatus(
                staff, 9L, new OrderDtos.StatusChangeRequest(OrderStatus.ACCEPTED, null)));
    }

    private CustomerOrder placedOrder(RestaurantBranch orderBranch) {
        CustomerOrder order = new CustomerOrder();
        order.setId(9L);
        order.setOrderNumber("HL-1008");
        order.setBranch(orderBranch);
        order.setOrderType(OrderType.PICKUP);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCurrency("INR");
        order.setEstimatedPrepMinutes(15);
        return order;
    }
}
