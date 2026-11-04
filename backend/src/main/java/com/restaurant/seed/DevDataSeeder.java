package com.restaurant.seed;

import com.restaurant.entity.BranchMenuItem;
import com.restaurant.entity.CustomerOrder;
import com.restaurant.entity.MenuCategory;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.OrderItem;
import com.restaurant.entity.OrderStatusHistory;
import com.restaurant.entity.Payment;
import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantBranch;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.OrderStatus;
import com.restaurant.enums.OrderType;
import com.restaurant.enums.PaymentStatus;
import com.restaurant.enums.UserRole;
import com.restaurant.repository.BranchMenuItemRepository;
import com.restaurant.repository.CustomerOrderRepository;
import com.restaurant.repository.MenuCategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.RestaurantBranchRepository;
import com.restaurant.repository.RestaurantRepository;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.service.OrderPricing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Development-only sample data. It does not run in the prod or test profiles.
 * Demo passwords are fictional and are documented in deployment.md.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final Set<String> INDIRANAGAR_HIDDEN = Set.of("Double Patty");
    private static final Set<String> BANDRA_HIDDEN = Set.of("Coastal Fish Curry", "Mushroom Truffle");

    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final BranchMenuItemRepository offerings;
    private final UserAccountRepository users;
    private final CustomerOrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            RestaurantRepository restaurants,
            RestaurantBranchRepository branches,
            MenuCategoryRepository categories,
            MenuItemRepository menuItems,
            BranchMenuItemRepository offerings,
            UserAccountRepository users,
            CustomerOrderRepository orders,
            PasswordEncoder passwordEncoder
    ) {
        this.restaurants = restaurants;
        this.branches = branches;
        this.categories = categories;
        this.menuItems = menuItems;
        this.offerings = offerings;
        this.users = users;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase("admin@hearthline.test")) {
            return;
        }
        Restaurant restaurant = restaurant();
        RestaurantBranch indiranagar = branch(restaurant, "Hearthline Indiranagar", "18, 100 Feet Road", "Bengaluru", "Karnataka", "560038", "9845011122", LocalTime.of(11, 0), LocalTime.of(23, 0), true);
        RestaurantBranch bandra = branch(restaurant, "Hearthline Bandra", "42, Turner Road", "Mumbai", "Maharashtra", "400050", "9820099344", LocalTime.of(11, 0), LocalTime.of(23, 30), true);
        RestaurantBranch delhi = branch(restaurant, "Hearthline Connaught Place", "7, Janpath", "New Delhi", "Delhi", "110001", "9810011223", LocalTime.of(12, 0), LocalTime.of(22, 30), false);
        List<MenuCategory> categoryList = List.of(
                category("Starters", "Small plates to begin", 1),
                category("Main Course", "Plates from the hearth", 2),
                category("Pizza", "Firewood pies", 3),
                category("Burgers", "Smashed and griddled", 4),
                category("Beverages", "Something to drink", 5),
                category("Desserts", "A last sweet bite", 6)
        );
        List<MenuItem> items = menuItems.saveAll(sampleItems(categoryList));
        for (MenuItem item : items) {
            offerings.save(offering(indiranagar, item, !INDIRANAGAR_HIDDEN.contains(item.getName())));
            offerings.save(offering(bandra, item, !BANDRA_HIDDEN.contains(item.getName())));
            offerings.save(offering(delhi, item, true));
        }
        UserAccount customer = user("Asha Raman", "customer@hearthline.test", "Customer@123", "9845001122", UserRole.CUSTOMER, null);
        UserAccount kitchen = user("Kabir Menon", "kitchen.indiranagar@hearthline.test", "Kitchen@123", "9845002211", UserRole.KITCHEN_STAFF, indiranagar);
        user("Meera Shah", "kitchen.bandra@hearthline.test", "Kitchen@123", "9820002211", UserRole.KITCHEN_STAFF, bandra);
        user("Neel Kapoor", "kitchen.delhi@hearthline.test", "Kitchen@123", "9810002211", UserRole.KITCHEN_STAFF, delhi);
        user("Hearthline Admin", "admin@hearthline.test", "Admin@123", "9800001100", UserRole.RESTAURANT_ADMIN, null);
        MenuItem pizza = items.stream().filter(item -> item.getName().equals("Margherita")).findFirst().orElseThrow();
        MenuItem paneer = items.stream().filter(item -> item.getName().equals("Butter Paneer")).findFirst().orElseThrow();
        MenuItem coffee = items.stream().filter(item -> item.getName().equals("Filter Coffee")).findFirst().orElseThrow();
        completedOrder(customer, kitchen, indiranagar, pizza);
        openOrder(customer, indiranagar, paneer, coffee);
        log.info("Development seed loaded. Demo logins are listed in deployment.md.");
    }

    private void completedOrder(UserAccount customer, UserAccount kitchen, RestaurantBranch branch, MenuItem item) {
        Instant placed = Instant.now().minus(1, ChronoUnit.DAYS);
        CustomerOrder order = baseOrder(customer, branch, OrderType.PICKUP, OrderStatus.COMPLETED, "14, Sample Lane, Bengaluru", null, placed);
        addLine(order, item, 1);
        finishMoney(order);
        order.attachPayment(payment(order, placed));
        stamp(order, customer, kitchen, placed, OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.COMPLETED);
        orders.save(order);
    }

    private void openOrder(UserAccount customer, RestaurantBranch branch, MenuItem first, MenuItem second) {
        Instant placed = Instant.now().minus(8, ChronoUnit.MINUTES);
        CustomerOrder order = baseOrder(customer, branch, OrderType.DELIVERY, OrderStatus.PLACED, null, "Leave it with the front desk.", placed);
        order.setDeliveryAddress("221, Lake View Apartments, Indiranagar");
        addLine(order, first, 1);
        addLine(order, second, 2);
        finishMoney(order);
        order.attachPayment(payment(order, placed));
        history(order, customer, null, OrderStatus.PLACED, "Order placed", placed);
        orders.save(order);
    }

    private CustomerOrder baseOrder(
            UserAccount customer,
            RestaurantBranch branch,
            OrderType type,
            OrderStatus status,
            String unusedAddress,
            String instructions,
            Instant when
    ) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("HL-" + orders.nextOrderNumber());
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setOrderType(type);
        order.setStatus(status);
        order.setCustomerName(customer.getFullName());
        order.setCustomerPhone(customer.getPhone());
        order.setSpecialInstructions(instructions);
        order.setCurrency("INR");
        order.setEstimatedPrepMinutes(20);
        order.setIdempotencyKey("seed-" + status.name().toLowerCase() + "-" + branch.getId());
        order.setIdempotencyFingerprint("seed");
        order.setCreatedAt(when);
        order.setUpdatedAt(when);
        return order;
    }

    private void addLine(CustomerOrder order, MenuItem item, int quantity) {
        OrderItem line = new OrderItem();
        line.setMenuItem(item);
        line.setItemName(item.getName());
        line.setUnitPrice(item.getPrice());
        line.setQuantity(quantity);
        line.setLineTotal(OrderPricing.lineTotal(item.getPrice(), quantity));
        line.setVegetarian(item.isVegetarian());
        order.addItem(line);
    }

    private void finishMoney(CustomerOrder order) {
        BigDecimal subtotal = order.getItems().stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = OrderPricing.tax(subtotal, new BigDecimal("0.05"));
        BigDecimal delivery = OrderPricing.deliveryFee(order.getOrderType(), new BigDecimal("40.00"));
        order.setSubtotal(OrderPricing.money(subtotal));
        order.setTaxAmount(tax);
        order.setDeliveryFee(delivery);
        order.setTotalAmount(OrderPricing.total(subtotal, tax, delivery));
    }

    private Payment payment(CustomerOrder order, Instant when) {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("INR");
        payment.setProvider("DEMO");
        payment.setProviderReference("DEMO-SEED-" + order.getOrderNumber());
        payment.setCreatedAt(when);
        payment.setUpdatedAt(when);
        return payment;
    }

    private void stamp(CustomerOrder order, UserAccount customer, UserAccount kitchen, Instant start, OrderStatus... statuses) {
        Instant cursor = start;
        OrderStatus previous = null;
        for (OrderStatus status : statuses) {
            UserAccount actor = status == OrderStatus.PLACED ? customer : kitchen;
            history(order, actor, previous, status, status.name().charAt(0) + status.name().substring(1).toLowerCase(), cursor);
            previous = status;
            cursor = cursor.plus(4, ChronoUnit.MINUTES);
        }
        order.setUpdatedAt(cursor);
    }

    private void history(CustomerOrder order, UserAccount actor, OrderStatus from, OrderStatus to, String note, Instant when) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setChangedBy(actor);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setNote(note);
        entry.setCreatedAt(when);
        order.addHistory(entry);
    }

    private Restaurant restaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Hearthline");
        restaurant.setDescription("A small restaurant chain for fire-cooked plates, ordered directly from the branch kitchen.");
        restaurant.setLogoUrl("https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=800&q=80");
        restaurant.setActive(true);
        return restaurants.save(restaurant);
    }

    private RestaurantBranch branch(
            Restaurant restaurant,
            String name,
            String address,
            String city,
            String state,
            String postal,
            String phone,
            LocalTime opens,
            LocalTime closes,
            boolean accepting
    ) {
        RestaurantBranch branch = new RestaurantBranch();
        branch.setRestaurant(restaurant);
        branch.setName(name);
        branch.setAddressLine(address);
        branch.setCity(city);
        branch.setState(state);
        branch.setPostalCode(postal);
        branch.setPhone(phone);
        branch.setOpensAt(opens);
        branch.setClosesAt(closes);
        branch.setActive(true);
        branch.setAcceptingOrders(accepting);
        return branches.save(branch);
    }

    private MenuCategory category(String name, String description, int order) {
        MenuCategory category = new MenuCategory();
        category.setName(name);
        category.setDescription(description);
        category.setDisplayOrder(order);
        category.setActive(true);
        return categories.save(category);
    }

    private BranchMenuItem offering(RestaurantBranch branch, MenuItem item, boolean available) {
        BranchMenuItem offering = new BranchMenuItem();
        offering.setBranch(branch);
        offering.setMenuItem(item);
        offering.setAvailable(available);
        return offering;
    }

    private UserAccount user(String name, String email, String password, String phone, UserRole role, RestaurantBranch branch) {
        UserAccount user = new UserAccount();
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(role);
        user.setBranch(branch);
        user.setEnabled(true);
        user.setTokenVersion(0);
        return users.save(user);
    }

    private List<MenuItem> sampleItems(List<MenuCategory> categories) {
        MenuCategory starters = categories.get(0);
        MenuCategory mains = categories.get(1);
        MenuCategory pizza = categories.get(2);
        MenuCategory burgers = categories.get(3);
        MenuCategory drinks = categories.get(4);
        MenuCategory desserts = categories.get(5);
        return List.of(
                item(starters, "Charred Corn Chaat", "Sweet corn, chilli, and lime off the grill.", "180.00", true, true, "photo-1546069901-ba9599a7e63c"),
                item(starters, "Pepper Chicken Wings", "Crisp wings with cracked pepper and honey.", "320.00", false, false, "photo-1527477396000-e27163b481c2"),
                item(starters, "Tomato Basil Soup", "Slow tomato soup with a basil oil finish.", "160.00", true, false, "photo-1547592166-23ac45744acd"),
                item(mains, "Butter Paneer", "Paneer in a warm tomato-butter gravy.", "340.00", true, true, "photo-1631452180519-c014fe946bc7"),
                item(mains, "Herb Roast Chicken", "Half chicken roasted with garlic and herbs.", "420.00", false, false, "photo-1598103442097-8b74394b95c6"),
                item(mains, "Mushroom Pot Pie", "A pastry lid over creamed mushrooms.", "310.00", true, false, "photo-1604908176997-125f25cc6f3d"),
                item(mains, "Coastal Fish Curry", "Fish simmered in coconut and tamarind.", "450.00", false, false, "photo-1626082927389-6cd097cdc6ec"),
                item(pizza, "Margherita", "Tomato, mozzarella, and basil on a blistered crust.", "399.00", true, true, "photo-1513104890138-7c749659a591"),
                item(pizza, "Firewood Pepperoni", "Pepperoni cups and chilli honey.", "499.00", false, false, "photo-1565299624946-b28f40a0ae38"),
                item(pizza, "Mushroom Truffle", "Roasted mushrooms and a little truffle oil.", "529.00", true, false, "photo-1571407970349-bc81e7e96d47"),
                item(burgers, "Classic Smash Burger", "Two smashed patties, onion, and cheddar.", "349.00", false, true, "photo-1550547660-d9450f859349"),
                item(burgers, "Paneer Tikka Burger", "Grilled paneer, mint, and pickled onion.", "299.00", true, false, "photo-1550317138-10000687a72b"),
                item(burgers, "Double Patty", "A taller stack with extra cheese.", "429.00", false, false, "photo-1571091718767-18b5b1457add"),
                item(drinks, "Filter Coffee", "South Indian filter coffee, served hot.", "120.00", true, false, "photo-1495474472287-4d71bcdd2085"),
                item(drinks, "Salted Lime Soda", "Lime, salt, and soda over ice.", "90.00", true, false, "photo-1513558161293-cdaf765ed2fd"),
                item(drinks, "Cold Cocoa", "Cocoa shaken with milk and a pinch of salt.", "180.00", true, false, "photo-1544145945-f90425340c7e"),
                item(desserts, "Burnt Cheesecake", "A baked cheesecake with a dark top.", "240.00", true, true, "photo-1565958011703-44f9829ba187"),
                item(desserts, "Gulab Jamun", "Two warm dumplings in light syrup.", "140.00", true, false, "photo-1666190092159-3171cf0fbb12"),
                item(desserts, "Chocolate Brownie", "A dense brownie with sea salt.", "190.00", true, false, "photo-1606313564200-e75d5e30476c"),
                item(desserts, "Seasonal Fruit Bowl", "Chilled fruit with lime and mint.", "160.00", true, false, "photo-1490474418585-ba9bad8fd0ea")
        );
    }

    private MenuItem item(MenuCategory category, String name, String description, String price, boolean vegetarian, boolean popular, String photo) {
        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setVegetarian(vegetarian);
        item.setPopular(popular);
        item.setActive(true);
        item.setImageUrl("https://images.unsplash.com/" + photo + "?auto=format&fit=crop&w=1200&q=80");
        return item;
    }
}
