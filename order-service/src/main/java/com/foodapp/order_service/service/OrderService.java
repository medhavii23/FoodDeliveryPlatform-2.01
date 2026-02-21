package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.constants.ErrorMessages;
import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.exception.InsufficientStockException;
import com.foodapp.order_service.exception.OrderNotFoundException;
import com.foodapp.order_service.exception.OrderProcessingException;
import com.foodapp.order_service.exception.StockFailure;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderItem;
import com.foodapp.order_service.model.OrderStatus;
import com.foodapp.order_service.repository.OrderItemRepository;
import com.foodapp.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
/**
 * Service layer component responsible for managing the complete
 * order lifecycle in the Food Delivery platform.
 *
 * <p>
 * This service acts as an orchestration layer coordinating between:
 * <ul>
 *   <li><b>Restaurant Service</b> – Inventory reservation & pricing</li>
 *   <li><b>Delivery Service</b> – Partner assignment & delivery charge</li>
 *   <li><b>Customer Service</b> – Customer validation</li>
 *   <li><b>Persistence Layer</b> – Order and OrderItem repositories</li>
 * </ul>
 *
 * <p>
 * Key Responsibilities:
 * <ul>
 *   <li>Place new orders with transactional consistency</li>
 *   <li>Reserve and release restaurant inventory</li>
 *   <li>Assign delivery partners with fallback handling</li>
 *   <li>Persist order and associated items</li>
 *   <li>Synchronize delivery updates</li>
 *   <li>Admin-based order status updates</li>
 *   <li>Fetch customer-specific order details and summaries</li>
 * </ul>
 *
 * <p>
 * Design Patterns Used:
 * <ul>
 *   <li><b>Orchestration Pattern</b> – Coordinates multiple downstream services</li>
 *   <li><b>Compensating Transaction</b> – Releases inventory on failure</li>
 *   <li><b>Fallback Pattern</b> – Handles delivery service failures gracefully</li>
 * </ul>
 *
 * <p>
 * All monetary values use {@link java.math.BigDecimal} with
 * centralized scaling and rounding rules defined in {@link Constants}.
 *
 * @author
 * @since 1.0
 */

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private RestaurantClient restaurantClient;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerService customerService;

    /**
     * Places a new order following a complete transactional workflow.
     *
     * <p><b>Execution Flow:</b>
     * <ol>
     *   <li>Validate customer existence</li>
     *   <li>Reserve inventory from Restaurant Service</li>
     *   <li>Assign delivery partner via Delivery Service</li>
     *   <li>Persist order and order items</li>
     *   <li>Release reserved inventory on failure</li>
     * </ol>
     *
     * <p>
     * Runs inside a database transaction. If any step fails after
     * successful inventory reservation, a compensating action is triggered
     * to release the reserved stock.
     *
     * @param request order placement request
     * @return persisted {@link Order}
     * @throws InsufficientStockException if reservation fails
     * @throws OrderProcessingException if downstream service fails
     */

    @Transactional
    public Order placeOrder(PlaceOrderRequest request) throws InsufficientStockException {

        log.info("Processing order for customer: {} at restaurant: {}",
                request.getCustomerName(), request.getRestaurantName());

        // Step 1: Ensure customer exists
        customerService.ensureCustomerExists(request.getCustomerId(), request.getCustomerName());

        // Step 2: Reserve inventory and get subtotal from Restaurant Service
        MenuReserveResponse reserveResp = reserveRestaurantInventory(request);

        UUID orderId = UUID.randomUUID();

        try {
            // Step 3: Assign delivery partner (gets partner, ETA, and delivery charge in
            // one call)
            DeliveryResponse deliveryResp = assignDeliveryPartner(orderId, reserveResp.getLocationName(),
                    request.getDeliveryArea());

            // Step 4: Save order with items
            Order order = saveOrderWithItems(orderId, request, reserveResp, deliveryResp);

            log.info("Order placed successfully: {} with total: {}", orderId, order.getTotalAmount());
            return order;

        } catch (Exception ex) {
            // Rollback inventory if order fails after reservation
            releaseInventory(request);
            throw new OrderProcessingException(ErrorMessages.ORDER_FAILED_ROLLBACK, ex);
        }
    }

    /**
     * Reserves requested menu items in the Restaurant Service.
     *
     * <p>
     * If reservation fails:
     * <ul>
     *   <li>Builds structured stock failure details</li>
     *   <li>Distinguishes between item-not-found and insufficient stock</li>
     *   <li>Throws {@link InsufficientStockException}</li>
     * </ul>
     *
     * @param request order request
     * @return successful reservation response
     * @throws InsufficientStockException if stock unavailable
     */

    private MenuReserveResponse reserveRestaurantInventory(PlaceOrderRequest request) {
        log.debug("Reserving inventory for restaurant: {}", request.getRestaurantName());

        MenuReserveRequest reserveReq = new MenuReserveRequest();
        reserveReq.setRestaurantName(request.getRestaurantName());
        reserveReq.setItems(request.getItems());

        MenuReserveResponse reserveResp = restaurantClient.reserve(reserveReq);

        if (reserveResp == null) {
            log.error("Restaurant service returned null response for: {}", request.getRestaurantName());
            throw new InsufficientStockException(ErrorMessages.INSUFFICIENT_STOCK, List.of());
        }

        // Check if reservation was successful
        if (!reserveResp.isSuccess()) {
            log.warn("Inventory reservation failed for restaurant: {}", request.getRestaurantName());
            List<StockFailure> failures = buildStockFailures(reserveResp);

            if (!failures.isEmpty()) {
                boolean anyNotFound = failures.stream()
                        .anyMatch(f -> f.getAvailable() == 0 && f.getItemId() == null);
                String msg = anyNotFound ? ErrorMessages.ITEM_NOT_FOUND : ErrorMessages.INSUFFICIENT_STOCK;
                throw new InsufficientStockException(msg, failures);
            }

            // Fallback to message from restaurant service
            String errorMsg = (reserveResp.getMessage() == null || reserveResp.getMessage().isBlank())
                    ? ErrorMessages.INSUFFICIENT_STOCK
                    : reserveResp.getMessage();
            throw new InsufficientStockException(errorMsg, List.of());
        }

        log.debug("Inventory reserved successfully. Subtotal: {}", reserveResp.getSubtotal());
        return reserveResp;
    }

    /**
     * Assigns a delivery partner using Delivery Service.
     *
     * <p>
     * Handles:
     * <ul>
     *   <li>Partner assignment</li>
     *   <li>Delivery charge calculation</li>
     *   <li>ETA estimation</li>
     * </ul>
     *
     * <p>
     * If the delivery service fails or returns unsuccessful response,
     * a fallback delivery response is generated to avoid blocking order creation.
     *
     * @param orderId unique order ID
     * @param restaurantLocation restaurant location
     * @param deliveryArea customer delivery area
     * @return delivery response (real or fallback)
     */

    private DeliveryResponse assignDeliveryPartner(UUID orderId,
            String restaurantLocation,
            String deliveryArea) {
        log.debug("Assigning delivery partner for order: {} from {} to {}",
                orderId, restaurantLocation, deliveryArea);

        DeliveryAssignRequest deliveryReq = new DeliveryAssignRequest(
                restaurantLocation,
                deliveryArea,
                null // Partner will be auto-assigned by delivery service
        );

        try {
            DeliveryResponse deliveryResp = deliveryClient.assignDelivery(orderId, deliveryReq);

            if (deliveryResp != null && deliveryResp.isSuccess()) {
                if (deliveryResp.getPartnerName() != null) {
                    log.info("Delivery partner assigned: {} for order: {}",
                            deliveryResp.getPartnerName(), orderId);
                } else {
                    log.info("Delivery assignment pending (10% fail case) for order: {}. Charge calculated: {}",
                            orderId, deliveryResp.getDeliveryCharge());
                }
                return deliveryResp;
            } else {
                log.warn("Delivery service marked request as failed for order: {}. Using fallback.", orderId);
                return createFallbackDeliveryResponse();
            }

        } catch (Exception ex) {
            log.error("Delivery service call failed for order: {}. Using fallback.", orderId, ex);
            return createFallbackDeliveryResponse();
        }
    }

    /**
     * Creates a fallback delivery response when Delivery Service
     * is unavailable or fails.
     *
     * <p>
     * Fallback values:
     * <ul>
     * <li>Partner: SEARCHING_PARTNER</li>
     * <li>Delivery charge: 0</li>
     * <li>ETA: PENDING</li>
     * <li>Status: false</li>
     * </ul>
     *
     * @return fallback {@link DeliveryResponse}
     */
    private DeliveryResponse createFallbackDeliveryResponse() {
        return new DeliveryResponse(
                Constants.SEARCHING_PARTNER,
                BigDecimal.ZERO,
                Constants.PENDING_ETA,
                false,
                Constants.DELIVERY_SERVICE_DELAY);
    }

    /**
     * Persists order entity and associated order items.
     *
     * <p>
     * Calculates:
     * <ul>
     *   <li>Food subtotal</li>
     *   <li>Delivery charge</li>
     *   <li>Total payable amount</li>
     * </ul>
     *
     * <p>
     * Order status rules:
     * <ul>
     *   <li>PLACED – Delivery partner assigned</li>
     *   <li>PREPARING – Partner pending</li>
     * </ul>
     *
     * @param orderId unique order ID
     * @param request original order request
     * @param reserveResp reservation response
     * @param deliveryResp delivery response
     * @return persisted {@link Order}
     */

    private Order saveOrderWithItems(UUID orderId,
            PlaceOrderRequest request,
            MenuReserveResponse reserveResp,
            DeliveryResponse deliveryResp) {
        log.debug("Saving order: {} with items", orderId);

        // Calculate totals
        BigDecimal subtotal = normalizeMoney(reserveResp.getSubtotal());
        BigDecimal deliveryCharge = normalizeMoney(deliveryResp.getDeliveryCharge());
        BigDecimal total = subtotal.add(deliveryCharge)
                .setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);


        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerId(request.getCustomerId());
        order.setRestaurantId(reserveResp.getRestaurantId());
        order.setFoodAmount(subtotal);
        order.setDeliveryCharge(deliveryCharge);
        order.setTotalAmount(total);

        OrderStatus status = deliveryResp.isSuccess() && deliveryResp.getPartnerName() != null
                ? OrderStatus.PLACED
                : OrderStatus.PREPARING;
        order.setStatus(status);
        order.setCreatedAt(Instant.now());

        orderRepository.save(order);

        if (reserveResp.getConfirmedItems() != null) {
            for (ConfirmedItem ci : reserveResp.getConfirmedItems()) {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setMenuItemId(ci.getItemId());
                oi.setItemName(ci.getItemName());
                oi.setUnitPrice(normalizeMoney(ci.getUnitPrice()));
                oi.setQty(ci.getQty());
                oi.setLineTotal(normalizeMoney(ci.getLineTotal()));
                orderItemRepository.save(oi);
            }
        }

        log.debug("Order saved successfully: {}", orderId);
        return order;
    }

    /**
     * Releases previously reserved inventory in Restaurant Service.
     *
     * <p>
     * This method implements a compensating transaction pattern.
     * It is executed when order placement fails after reservation.
     *
     * <p>
     * Any exception during release is logged but not rethrown
     * to prevent cascading failures.
     *
     * @param request original order request
     */

    private void releaseInventory(PlaceOrderRequest request) {
        try {
            log.info("Releasing inventory for restaurant: {}", request.getRestaurantName());
            MenuReserveRequest releaseReq = new MenuReserveRequest();
            releaseReq.setRestaurantName(request.getRestaurantName());
            releaseReq.setItems(request.getItems());

            restaurantClient.release(releaseReq);
            log.debug("Inventory released successfully");
        } catch (Exception ex) {
            log.error("Failed to release inventory for restaurant: {}",
                    request.getRestaurantName(), ex);
            // Don't throw - this is best effort cleanup
        }
    }

    /**
     * Converts unavailable items from Restaurant Service
     * into structured {@link StockFailure} objects.
     *
     * @param reserveResp reservation response
     * @return list of stock failure details
     */
    private List<StockFailure> buildStockFailures(MenuReserveResponse reserveResp) {
        List<StockFailure> failures = new ArrayList<>();

        if (reserveResp.getUnavailableItems() != null) {
            for (UnavailableItem ui : reserveResp.getUnavailableItems()) {
                failures.add(new StockFailure(
                        ui.getItemId(),
                        ui.getItemName(),
                        ui.getRequestedQty(),
                        ui.getAvailableQty()));
            }
        }

        return failures;
    }

    /**
     * Updates the status of an existing order.
     *
     * <p>
     * Intended for administrative operations.
     * Security validation should be enforced at controller layer.
     *
     * @param orderId order ID
     * @param newStatus new status
     * @return updated order
     * @throws OrderNotFoundException if order not found
     */

    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        log.info("Updating order status: {} to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == newStatus) {
            log.debug("Order status already set to: {}", newStatus);
            return order;
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status updated successfully: {} -> {}", orderId, newStatus);
        return updatedOrder;
    }
    /**
     * Synchronizes order data from Delivery Service callback.
     *
     * <p>
     * Supports partial updates:
     * <ul>
     *   <li>Delivery charge update (recalculates total)</li>
     *   <li>Status update</li>
     *   <li>Automatic PREPARING → PLACED transition when partner assigned</li>
     * </ul>
     *
     * @param syncReq delivery synchronization request
     * @return updated order
     */

    @Transactional
    public Order syncOrder(OrderSyncRequest syncReq) {
        log.info("Syncing order: {}", syncReq.getOrderId());

        Order order = orderRepository.findById(syncReq.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        ErrorMessages.ORDER_NOT_FOUND_ID + syncReq.getOrderId()));

        boolean updated = false;

        if (syncReq.getDeliveryCharge() != null) {
            BigDecimal dc = normalizeMoney(syncReq.getDeliveryCharge());
            order.setDeliveryCharge(dc);

            BigDecimal foodAmount = order.getFoodAmount() != null
                    ? order.getFoodAmount()
                    : BigDecimal.ZERO;

            order.setTotalAmount(
                    normalizeMoney(foodAmount).add(dc)
                            .setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING)
            );
            updated = true;
        }


        if (syncReq.getStatus() != null) {
            order.setStatus(syncReq.getStatus());
            updated = true;
        } else if (syncReq.getPartnerName() != null && order.getStatus() == OrderStatus.PREPARING) {
            // Auto-transition to PLACED when partner is assigned
            order.setStatus(OrderStatus.PLACED);
        }

        if (updated) {
            order = orderRepository.save(order);
            log.info("Order synced successfully: {}", syncReq.getOrderId());
        } else {
            log.debug("No changes to sync for order: {}", syncReq.getOrderId());
        }

        return order;
    }

    /**
     * Retrieves all orders for a specific customer,
     * sorted by newest first.
     *
     * @param customerId customer ID
     * @return list of orders
     */

    public List<Order> getUserOrders(UUID customerId) {
        log.debug("getUserOrders customerId: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Retrieves a specific order belonging to a customer.
     *
     * <p>
     * Prevents access to orders owned by other users.
     *
     * @param orderId order ID
     * @param customerId customer ID
     * @return matching order
     * @throws OrderNotFoundException if not found or unauthorized
     */

    public Order getUserOrder(UUID orderId, UUID customerId) {
        log.debug("getUserOrder orderId: {} customerId: {}", orderId, customerId);
        return orderRepository.findByOrderIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Retrieves summarized order details for a customer.
     *
     * <p>
     * Data is fetched via join query and mapped into
     * {@link CustomerOrderSummaryDto}.
     *
     * @param customerId customer ID
     * @return list of summarized orders
     */

    public List<CustomerOrderSummaryDto> getCustomerOrderSummary(UUID customerId) {
        log.debug("getCustomerOrderSummary customerId: {}", customerId);
        List<Object[]> rows = orderRepository.findCustomerOrderSummaryByCustomerId(customerId);
        return rows.stream().map(this::mapRowToCustomerOrderSummary).toList();
    }

    private CustomerOrderSummaryDto mapRowToCustomerOrderSummary(Object[] row) {
        CustomerOrderSummaryDto dto = new CustomerOrderSummaryDto();

        dto.setOrderId(toUUID(row[0]));
        dto.setCustomerId(toUUID(row[1]));
        dto.setTotalAmount(toBigDecimal(row[2]));
        dto.setOrderStatus(toOrderStatus(row[3]));
        dto.setCreatedAt(toInstant(row[4]));
        dto.setRestaurantId(toLong(row[5]));
        dto.setRestaurantName(toStringValue(row[6]));
        dto.setLocationName(toStringValue(row[7]));
        dto.setDeliveryId(toLong(row[8]));
        dto.setDeliveryPartner(toStringValue(row[9]));
        dto.setDeliveryEta(toStringValue(row[10]));
        dto.setDeliveryStatus(toStringValue(row[11]));

        return dto;
    }

    private UUID toUUID(Object value) {
        return value != null ? UUID.fromString(value.toString()) : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        return value != null ? new BigDecimal(value.toString()) : null;
    }

    private OrderStatus toOrderStatus(Object value) {
        return value != null ? OrderStatus.valueOf(value.toString()) : null;
    }

    private Long toLong(Object value) {
        return value != null ? ((Number) value).longValue() : null;
    }

    private String toStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }

        if (value instanceof Instant i) {
            return i;
        }

        return java.sql.Timestamp.valueOf(value.toString()).toInstant();
    }


    /**
     * Normalizes monetary value using configured scale and rounding mode.
     *
     * <p>
     * If value is null, defaults to {@link BigDecimal#ZERO}.
     *
     * @param value monetary value
     * @return normalized monetary value
     */

    BigDecimal normalizeMoney(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(Constants.MONEY_SCALE,
                Constants.MONEY_ROUNDING);
    }
}
