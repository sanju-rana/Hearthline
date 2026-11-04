package com.restaurant.websocket;

import com.restaurant.entity.CustomerOrder;
import com.restaurant.enums.UserRole;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.security.RestaurantUserDetails;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SubscriptionGuard implements ChannelInterceptor {

    private static final Pattern KITCHEN = Pattern.compile("^/topic/kitchen/(\\d+)$");
    private static final Pattern ORDER = Pattern.compile("^/topic/orders/(\\d+)$");

    private final CustomerOrderRepository orders;

    public SubscriptionGuard(CustomerOrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        RestaurantUserDetails user = currentUser(accessor);
        if (user == null || !allowed(user, accessor.getDestination())) {
            throw new AccessDeniedException("You are not authorized to access this order.");
        }
        return message;
    }

    private boolean allowed(RestaurantUserDetails user, String destination) {
        if (destination == null) {
            return false;
        }
        Matcher kitchen = KITCHEN.matcher(destination);
        if (kitchen.matches()) {
            long branchId = Long.parseLong(kitchen.group(1));
            return user.getRole() == UserRole.RESTAURANT_ADMIN
                    || (user.getRole() == UserRole.KITCHEN_STAFF && branchId == user.getBranchId());
        }
        Matcher order = ORDER.matcher(destination);
        if (order.matches()) {
            long orderId = Long.parseLong(order.group(1));
            CustomerOrder found = orders.findById(orderId).orElse(null);
            if (found == null) {
                return false;
            }
            if (user.getRole() == UserRole.RESTAURANT_ADMIN) {
                return true;
            }
            if (user.getRole() == UserRole.CUSTOMER) {
                return found.getCustomer().getId().equals(user.getId());
            }
            return user.getRole() == UserRole.KITCHEN_STAFF
                    && found.getBranch().getId().equals(user.getBranchId());
        }
        return false;
    }

    private RestaurantUserDetails currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.getPrincipal() instanceof RestaurantUserDetails details) {
            return details;
        }
        return null;
    }
}
