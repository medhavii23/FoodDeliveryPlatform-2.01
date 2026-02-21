package com.foodapp.order_service.controller;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.AddToCartResponse;
import com.foodapp.order_service.dto.CartItemUpdateRequest;
import com.foodapp.order_service.dto.CartViewResponse;
import com.foodapp.order_service.dto.CustomerOrderSummaryDto;
import com.foodapp.order_service.dto.OrderSyncRequest;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderStatus;
import com.foodapp.order_service.service.CartService;
import com.foodapp.order_service.service.CustomerService;
import com.foodapp.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for cart and order operations.
 *
 * <p>
 * Exposes endpoints for:
 * <ul>
 * <li>Adding/updating/removing cart items</li>
 * <li>Viewing and clearing the active cart</li>
 * <li>Checkout (convert cart to order)</li>
 * <li>Listing and viewing customer orders</li>
 * <li>Order summary, sync, and admin status updates</li>
 * </ul>
 *
 * <p>
 * Cart and order operations require {@code X-Auth-Id} (customer UUID) in
 * headers.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    // --- Cart Operations ---

    /**
     * Adds or updates an item in the customer's cart for a restaurant.
     *
     * @param request            item update (restaurant name, delivery area, item
     *                           name, quantity)
     * @param authUser           optional user name from auth
     * @param authId             customer UUID (required)
     * @param customerNameHeader optional customer name from header
     * @return add-to-cart response with cart summary
     */
    @PostMapping("/items")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AddToCartResponse addOrUpdateItem(
            @Valid @RequestBody CartItemUpdateRequest request,
            @RequestHeader(value = Constants.AUTH_USER, required = false) String authUser,
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID authId,
            @RequestHeader(value = Constants.CUSTOMER_NAME, required = false) String customerNameHeader) {

        UUID customerId = authId;
        String name = authUser != null ? authUser : customerNameHeader;

        if (customerId == null) {
            log.debug("Rejected add/update cart: missing X-Auth-Id");
            throw new IllegalArgumentException("Customer id (X-Auth-Id) is required for cart operations");
        }

        log.info("Add/update cart item for customer: {} restaurant: {} item: {}",
                customerId, request.getRestaurantName(), request.getItemName());
        return cartService.addOrUpdateItem(customerId, name, request);
    }

    /**
     * Returns the customer's active cart, optionally filtered by restaurant.
     *
     * @param authUser       optional user name from auth
     * @param authId         customer UUID (required)
     * @param restaurantName optional filter by restaurant name
     * @return cart view or null if no active cart
     */
    @GetMapping("/my")
    public CartViewResponse myCart(
            @RequestHeader(value = Constants.AUTH_USER, required = false) String authUser,
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID authId,
            @RequestParam(value = "restaurantName", required = false) String restaurantName) {

        if (authId == null) {
            log.debug("Rejected get cart: missing X-Auth-Id");
            throw new IllegalArgumentException("Customer id (X-Auth-Id) is required for cart operations");
        }

        log.debug("Get active cart for customer: {} restaurantName: {}", authId, restaurantName);
        return cartService.getActiveCart(authId, restaurantName);
    }

    /**
     * Clears (cancels) the customer's active cart.
     *
     * @param authId         customer UUID (required)
     * @param restaurantName optional filter by restaurant name
     */
    @DeleteMapping("/my")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMyCart(
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID authId,
            @RequestParam(value = "restaurantName", required = false) String restaurantName) {

        if (authId == null) {
            log.debug("Rejected clear cart: missing X-Auth-Id");
            throw new IllegalArgumentException("Customer id (X-Auth-Id) is required for cart operations");
        }

        log.info("Clear active cart for customer: {}", authId);
        cartService.clearActiveCart(authId, restaurantName);
    }

    /**
     * Converts the active cart to an order (checkout).
     *
     * @param authUser           optional user name from auth
     * @param authId             customer UUID (required)
     * @param customerNameHeader optional customer name from header
     * @param restaurantName     optional filter by restaurant
     * @return the created order
     */
    @PostMapping("/checkout")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Order checkout(
            @Valid @RequestHeader(value = Constants.AUTH_USER, required = false) String authUser,
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID authId,
            @RequestHeader(value = Constants.CUSTOMER_NAME, required = false) String customerNameHeader,
            @RequestParam(value = "restaurantName", required = false) String restaurantName) {

        if (authId == null) {
            log.debug("Rejected checkout: missing X-Auth-Id");
            throw new IllegalArgumentException("Customer id (X-Auth-Id) is required for cart operations");
        }

        String name = authUser != null ? authUser : customerNameHeader;
        log.info("Checkout for customer: {}", authId);
        return cartService.checkout(authId, name, restaurantName);
    }

    // --- Order Operations (Consolidated) ---

    /**
     * Returns all orders for the authenticated customer.
     *
     * @param authUser     optional user name from auth
     * @param customerId   customer UUID
     * @param customerName optional customer name from header
     * @return list of orders (newest first)
     */
    @GetMapping("/orders")
    public List<Order> myOrders(
            @RequestHeader(value = Constants.AUTH_USER, required = false) String authUser,
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID customerId,
            @RequestHeader(value = Constants.CUSTOMER_NAME, required = false) String customerName) {

        String effectiveName = authUser != null ? authUser : customerName;
        if (effectiveName != null && customerId != null) {
            customerService.verifyOrThrow(customerId, effectiveName);
        }
        log.debug("List orders for customer: {}", customerId);
        return orderService.getUserOrders(customerId);
    }

    /**
     * Returns a single order by ID for the authenticated customer.
     *
     * @param orderId      order UUID
     * @param authUser     optional user name from auth
     * @param customerId   customer UUID
     * @param customerName optional customer name from header
     * @return the order if found and owned by customer
     */
    @GetMapping("/orders/{orderId}")
    public Order myOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = Constants.AUTH_USER, required = false) String authUser,
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID customerId,
            @RequestHeader(value = Constants.CUSTOMER_NAME, required = false) String customerName) {

        String effectiveName = authUser != null ? authUser : customerName;
        if (effectiveName != null && customerId != null) {
            customerService.verifyOrThrow(customerId, effectiveName);
        }
        log.debug("Get order: {} for customer: {}", orderId, customerId);
        return orderService.getUserOrder(orderId, customerId);
    }

    /**
     * Returns customer order summary with restaurant name and delivery partner
     * (single query, normalized joins).
     *
     * @param customerId customer UUID (required)
     * @return list of order summary DTOs
     */
    @GetMapping("/orders/summary")
    public List<CustomerOrderSummaryDto> orderSummary(
            @RequestHeader(value = Constants.AUTH_ID, required = false) UUID customerId) {

        if (customerId == null) {
            log.debug("Rejected order summary: missing X-Auth-Id");
            throw new IllegalArgumentException("Customer id (X-Auth-Id) is required");
        }
        log.debug("Order summary for customer: {}", customerId);
        return orderService.getCustomerOrderSummary(customerId);
    }

    /**
     * Syncs order from delivery service (e.g. partner assigned, ETA, status).
     *
     * @param syncReq sync payload with orderId and optional delivery/status fields
     * @return updated order
     */
    @PostMapping("/orders/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Order syncOrder(@RequestBody OrderSyncRequest syncReq) {
        log.info("Order sync received for order: {}", syncReq.getOrderId());
        return orderService.syncOrder(syncReq);
    }

    /**
     * Updates order status (admin only).
     *
     * @param role    caller role (must be admin)
     * @param orderId order UUID
     * @param status  new status
     * @return updated order
     */
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Order updateStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status) {
        return orderService.updateOrderStatus(orderId, status);
    }
}
