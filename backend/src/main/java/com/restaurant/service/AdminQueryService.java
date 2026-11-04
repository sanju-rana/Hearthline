package com.restaurant.service;

import com.restaurant.config.AppProperties;
import com.restaurant.dto.AdminDtos;
import com.restaurant.dto.OrderDtos;
import com.restaurant.dto.PageResponse;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.Payment;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.RestaurantBranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminQueryService {

    private static final List<OrderStatus> PENDING = List.of(
            OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY);
    private static final List<OrderStatus> EXCLUDED_FROM_REVENUE = List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED);

    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;
    private final RestaurantBranchRepository branches;
    private final BranchMenuItemRepository offerings;
    private final AppProperties properties;

    public AdminQueryService(
            CustomerOrderRepository orders,
            PaymentRepository payments,
            RestaurantBranchRepository branches,
            BranchMenuItemRepository offerings,
            AppProperties properties
    ) {
        this.orders = orders;
        this.payments = payments;
        this.branches = branches;
        this.offerings = offerings;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public AdminDtos.DashboardResponse dashboard() {
        ZoneId zone = ZoneId.of(properties.order().businessZone());
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Map<OrderStatus, Long> counts = orders.countGroupedByStatus().stream()
                .collect(Collectors.toMap(row -> (OrderStatus) row[0], row -> (Long) row[1]));
        List<AdminDtos.StatusCount> byStatus = Arrays.stream(OrderStatus.values())
                .map(status -> new AdminDtos.StatusCount(status, counts.getOrDefault(status, 0L)))
                .toList();
        BigDecimal revenue = payments.sumAmount(PaymentStatus.SUCCESS, EXCLUDED_FROM_REVENUE);
        return new AdminDtos.DashboardResponse(
                orders.count(),
                orders.countByCreatedAtGreaterThanEqual(startOfDay),
                orders.countByStatusIn(PENDING),
                counts.getOrDefault(OrderStatus.COMPLETED, 0L),
                revenue == null ? new BigDecimal("0.00") : revenue,
                branches.countByActiveTrue(),
                offerings.countAvailableOfferings(),
                byStatus);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderSummaryResponse> orders(OrderStatus status, Long branchId, String orderNumber, int page, int size) {
        String number = orderNumber == null || orderNumber.isBlank() ? null : orderNumber.trim();
        var result = orders.search(status, branchId, number, Pages.of(page, size));
        Map<Long, Payment> paymentByOrder = payments.findByOrderIdIn(
                result.getContent().stream().map(CustomerOrder::getId).toList()).stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), Function.identity()));
        List<OrderDtos.OrderSummaryResponse> content = result.getContent().stream()
                .map(order -> ViewMapper.toSummary(order, paymentByOrder.get(order.getId())))
                .toList();
        return Pages.wrap(result, content);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderDetailResponse order(Long orderId) {
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        order.getItems().size();
        order.getHistory().size();
        order.getBranch().getName();
        if (order.getPayment() != null) {
            order.getPayment().getStatus();
        }
        return ViewMapper.toDetail(order);
    }
}
