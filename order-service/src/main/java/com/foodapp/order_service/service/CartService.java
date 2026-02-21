package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.Cart;
import com.foodapp.order_service.model.CartItem;
import com.foodapp.order_service.model.CartStatus;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.repository.CartItemRepository;
import com.foodapp.order_service.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing a customer's shopping cart lifecycle.
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Creating an active cart per customer per restaurant (case-insensitive)</li>
 *   <li>Adding/updating/removing items from the active cart</li>
 *   <li>Viewing cart summary (items + subtotal, delivery charge, total)</li>
 *   <li>Clearing/cancelling an active cart</li>
 *   <li>Checkout: converting the active cart into an Order via {@link OrderService}</li>
 * </ul>
 *
 * <p>Pricing/subtotal is retrieved by calling Restaurant Service quote endpoint through
 * {@link RestaurantClient}. If quote fails, cart is still returned but with 0 pricing.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RestaurantClient restaurantClient;

    @Autowired
    private DeliveryClient deliveryClient;

    /**
     * Adds a new item to the customer's active cart for a given restaurant, or updates an existing item's quantity.
     *
     * <p>Rules:
     * <ul>
     *   <li>If an ACTIVE cart for the same customer + restaurant exists, it is reused.</li>
     *   <li>If no cart exists, a new ACTIVE cart is created.</li>
     *   <li>If {@code qty} is {@code null} or {@code <= 0}, the item is removed from cart (if present).</li>
     * </ul>
     *
     * <p>This method validates the customer identity by calling
     * {@link CustomerService#ensureCustomerExists(UUID, String)}.
     *
     * @param customerId the customer UUID (from auth/gateway context)
     * @param customerName the customer name (from auth/gateway context)
     * @param request request containing restaurant name, delivery area, item name and quantity
     * @return add-to-cart response (no delivery charge or ETA)
     */
    @Transactional
    public AddToCartResponse addOrUpdateItem(UUID customerId, String customerName, CartItemUpdateRequest request) {
        log.debug("addOrUpdateItem customer: {} restaurant: {} item: {} qty: {}",
                customerId, request.getRestaurantName(), request.getItemName(), request.getQty());

        customerService.ensureCustomerExists(customerId, customerName);

        RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantByName(request.getRestaurantName());
        Long restaurantId = restaurantInfo.getRestaurantId();

        Cart cart = cartRepository
                .findByCustomerIdAndRestaurantIdAndStatus(customerId, restaurantId, CartStatus.ACTIVE)
                .orElseGet(() -> newCart(customerId, restaurantId, request));

        if (request.getQty() == null || request.getQty() <= 0) {
            cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(cart.getCartId(), request.getItemName())
                    .ifPresent(cartItemRepository::delete);
        } else {
            CartItem item = cartItemRepository
                    .findByCart_CartIdAndItemNameIgnoreCase(cart.getCartId(), request.getItemName())
                    .orElseGet(() -> {
                        CartItem ci = new CartItem();
                        ci.setCart(cart);
                        ci.setItemName(request.getItemName());
                        return ci;
                    });
            item.setQty(request.getQty());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        log.info("Cart item updated for customer: {} cart: {}", customerId, cart.getCartId());
        return toAddToCartView(cart, customerName);
    }

    /**
     * Returns the customer's current ACTIVE cart.
     *
     * <p>If {@code restaurantName} is provided, it fetches the ACTIVE cart scoped to that restaurant.
     * Otherwise, it fetches any ACTIVE cart for the customer.
     *
     * @param customerId the customer UUID
     * @param restaurantName optional restaurant name filter; may be null/blank
     * @return a {@link CartViewResponse} for the active cart, or {@code null} if no active cart exists
     */
    public CartViewResponse getActiveCart(UUID customerId, String restaurantName) {
        log.debug("getActiveCart customer: {} restaurantName: {}", customerId, restaurantName);
        Cart cart;
        if (restaurantName != null && !restaurantName.isBlank()) {
            try {
                RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantByName(restaurantName);
                cart = cartRepository
                        .findByCustomerIdAndRestaurantIdAndStatus(customerId, restaurantInfo.getRestaurantId(), CartStatus.ACTIVE)
                        .orElse(null);
            } catch (Exception _) {
                cart = null;
            }
        } else {
            cart = cartRepository
                    .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                    .orElse(null);
        }

        if (cart == null) {
            log.debug("No active cart for customer: {}", customerId);
            return null;
        }

        return toViewWithDelivery(cart, null);
    }

    /**
     * Clears (cancels) the customer's ACTIVE cart.
     *
     * <p>Implementation:
     * <ul>
     *   <li>Deletes all {@link CartItem} rows for the cart</li>
     *   <li>Marks the cart {@link CartStatus#CANCELLED}</li>
     * </ul>
     *
     * @param customerId the customer UUID
     * @param restaurantName optional restaurant name filter; may be null/blank
     */
    @Transactional
    public void clearActiveCart(UUID customerId, String restaurantName) {
        log.debug("clearActiveCart customer: {} restaurantName: {}", customerId, restaurantName);
        Cart cart;
        if (restaurantName != null && !restaurantName.isBlank()) {
            try {
                RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantByName(restaurantName);
                cart = cartRepository
                        .findByCustomerIdAndRestaurantIdAndStatus(customerId, restaurantInfo.getRestaurantId(), CartStatus.ACTIVE)
                        .orElse(null);
            } catch (Exception _) {
                cart = null;
            }
        } else {
            cart = cartRepository
                    .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                    .orElse(null);
        }

        if (cart == null) {
            log.debug("No active cart to clear for customer: {}", customerId);
            return;
        }

        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.setStatus(CartStatus.CANCELLED);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        log.info("Cart cleared for customer: {} cart: {}", customerId, cart.getCartId());
    }

    /**
     * Converts the customer's ACTIVE cart into an Order by delegating to {@link OrderService#placeOrder(PlaceOrderRequest)}.
     *
     * <p>Process:
     * <ol>
     *   <li>Fetch ACTIVE cart (optionally scoped to restaurant)</li>
     *   <li>Fetch cart items; fail if empty</li>
     *   <li>Build {@link PlaceOrderRequest} and call {@link OrderService}</li>
     *   <li>Mark cart {@link CartStatus#CHECKED_OUT} and store created orderId</li>
     * </ol>
     *
     * <p>Note: Cart items are not deleted after checkout, so you can retain history.
     *
     * @param customerId the customer UUID
     * @param customerName the customer name (passed into order for snapshot/history)
     * @param restaurantNameOverride optional restaurant name filter; may be null/blank
     * @return the newly created {@link Order}
     * @throws IllegalStateException if no active cart exists or cart has no items
     */
    @Transactional
    public Order checkout(UUID customerId, String customerName, String restaurantNameOverride) {
        log.info("Checkout started for customer: {} restaurantFilter: {}", customerId, restaurantNameOverride);
        Cart cart;
        if (restaurantNameOverride != null && !restaurantNameOverride.isBlank()) {
            try {
                RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantByName(restaurantNameOverride);
                cart = cartRepository
                        .findByCustomerIdAndRestaurantIdAndStatus(customerId, restaurantInfo.getRestaurantId(), CartStatus.ACTIVE)
                        .orElse(null);
            } catch (Exception _) {
                cart = null;
            }
        } else {
            cart = cartRepository
                    .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                    .orElse(null);
        }

        if (cart == null) {
            log.warn("Checkout failed: no active cart for customer: {}", customerId);
            throw new IllegalStateException("No active cart for customer");
        }

        List<CartItem> items = cartItemRepository.findByCart_CartId(cart.getCartId());
        if (items.isEmpty()) {
            log.warn("Checkout failed: empty cart for customer: {}", customerId);
            throw new IllegalStateException("Cart is empty");
        }

        RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantById(cart.getRestaurantId());
        PlaceOrderRequest placeReq = new PlaceOrderRequest();
        placeReq.setCustomerId(customerId);
        placeReq.setCustomerName(customerName);
        placeReq.setRestaurantName(restaurantInfo.getRestaurantName());
        placeReq.setDeliveryArea(cart.getDeliveryArea());
        placeReq.setItems(
                items.stream().map(ci -> {
                    ItemNameQty inq = new ItemNameQty();
                    inq.setItemName(ci.getItemName());
                    inq.setQty(ci.getQty());
                    return inq;
                }).collect(Collectors.toList()));

        Order order = orderService.placeOrder(placeReq);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.setOrderId(order.getOrderId());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        log.info("Checkout completed for customer: {} order: {}", customerId, order.getOrderId());
        return order;
    }

    /**
     * Creates a new ACTIVE cart for the customer and persists it.
     *
     * @param customerId the customer UUID
     * @param restaurantId the restaurant id
     * @param request incoming request containing restaurant name and delivery area
     * @return persisted {@link Cart} entity
     */
    private Cart newCart(UUID customerId, Long restaurantId, CartItemUpdateRequest request) {
        Cart cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setCustomerId(customerId);
        cart.setRestaurantId(restaurantId);
        cart.setDeliveryArea(request.getDeliveryArea());
        cart.setStatus(CartStatus.ACTIVE);
        Instant now = Instant.now();
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        return cartRepository.save(cart);
    }

    /**
     * Shared view data: restaurant info, item views, and subtotal from restaurant quote.
     */
    private static class ViewData {
        final RestaurantInfoResponse restaurantInfo;
        final List<CartItemView> itemViews;
        final BigDecimal subtotal;

        ViewData(RestaurantInfoResponse restaurantInfo, List<CartItemView> itemViews, BigDecimal subtotal) {
            this.restaurantInfo = restaurantInfo;
            this.itemViews = itemViews;
            this.subtotal = subtotal;
        }
    }

    /**
     * Loads cart items, fetches restaurant quote, and returns base view data (no delivery).
     */
    private ViewData buildViewData(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartItemView> itemViews;

        RestaurantInfoResponse restaurantInfo = restaurantClient.getRestaurantById(cart.getRestaurantId());
        String restaurantName = restaurantInfo != null ? restaurantInfo.getRestaurantName() : null;

        MenuReserveRequest quoteReq = new MenuReserveRequest();
        quoteReq.setRestaurantName(restaurantName);
        quoteReq.setItems(cartItems.stream()
                .map(ci -> {
                    ItemNameQty inq = new ItemNameQty();
                    inq.setItemName(ci.getItemName());
                    inq.setQty(ci.getQty());
                    return inq;
                })
                .collect(Collectors.toList()));

        try {
            MenuReserveResponse quoteResp = restaurantClient.quote(quoteReq);
            if (quoteResp != null && quoteResp.isSuccess() && quoteResp.getConfirmedItems() != null) {
                itemViews = quoteResp.getConfirmedItems().stream()
                        .map(ci -> new CartItemView(
                                ci.getItemName(),
                                ci.getQty(),
                                normalizeMoney(ci.getUnitPrice()),
                                normalizeMoney(ci.getLineTotal())))
                        .collect(Collectors.toList());
                subtotal = normalizeMoney(quoteResp.getSubtotal());
            } else {
                itemViews = cartItems.stream()
                        .map(ci -> new CartItemView(ci.getItemName(), ci.getQty(), BigDecimal.ZERO, BigDecimal.ZERO))
                        .collect(Collectors.toList());
            }
        } catch (Exception _) {
            itemViews = cartItems.stream()
                    .map(ci -> new CartItemView(ci.getItemName(), ci.getQty(), BigDecimal.ZERO, BigDecimal.ZERO))
                    .collect(Collectors.toList());
        }

        BigDecimal scaledSubtotal = subtotal.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
        return new ViewData(restaurantInfo, itemViews, scaledSubtotal);
    }

    /**
     * Builds add-to-cart response (no delivery charge or ETA).
     */
    private AddToCartResponse toAddToCartView(Cart cart, String customerName) {
        ViewData data = buildViewData(cart);
        String restaurantName = data.restaurantInfo != null ? data.restaurantInfo.getRestaurantName() : null;
        BigDecimal total = data.subtotal;
        return new AddToCartResponse(
                cart.getCartId(),
                cart.getCustomerId(),
                customerName,
                restaurantName,
                cart.getDeliveryArea(),
                cart.getStatus(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                data.itemViews,
                data.subtotal,
                total);
    }

    /**
     * Builds view-cart response with delivery estimate (deliveryCharge, eta, total).
     */
    private CartViewResponse toViewWithDelivery(Cart cart, String customerName) {
        ViewData data = buildViewData(cart);
        String restaurantName = data.restaurantInfo != null ? data.restaurantInfo.getRestaurantName() : null;

        BigDecimal deliveryCharge = BigDecimal.ZERO;
        String eta = null;

        String restaurantLocation = data.restaurantInfo != null ? data.restaurantInfo.getLocationName() : null;
        String customerLocation = cart.getDeliveryArea();
        if (restaurantLocation != null && !restaurantLocation.isBlank() && customerLocation != null && !customerLocation.isBlank()) {
            try {
                DeliveryAssignRequest deliveryReq = new DeliveryAssignRequest(restaurantLocation, customerLocation, null);
                DeliveryResponse deliveryResp = deliveryClient.estimateDelivery(deliveryReq);
                if (deliveryResp != null && deliveryResp.isSuccess()) {
                    deliveryCharge = normalizeMoney(deliveryResp.getDeliveryCharge());
                    eta = deliveryResp.getEta();
                }
            } catch (Exception _) {
                // keep deliveryCharge=0, eta=null
            }
        }

        BigDecimal scaledDelivery = deliveryCharge.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
        BigDecimal total = data.subtotal.add(scaledDelivery).setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);

        return new CartViewResponse(
                cart.getCartId(),
                cart.getCustomerId(),
                customerName,
                restaurantName,
                cart.getDeliveryArea(),
                cart.getStatus(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                data.itemViews,
                data.subtotal,
                scaledDelivery,
                eta,
                total);
    }

    /**
     * Normalizes money values to the application's configured scale and rounding mode.
     *
     * <p>If the provided value is null, it is treated as {@link BigDecimal#ZERO}.
     *
     * @param value money amount (nullable)
     * @return normalized money amount with scale + rounding applied
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO)
                .setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
    }
}
