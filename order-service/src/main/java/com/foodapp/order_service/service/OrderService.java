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
 * Core business service responsible for complete order lifecycle management
 * in the food delivery platform.
 *
 * <p>This service orchestrates communication between:
 * <ul>
 *   <li>Restaurant Service (inventory reservation & pricing)</li>
 *   <li>Delivery Service (partner assignment, ETA, delivery charge)</li>
 *   <li>Customer validation layer</li>
 * </ul>
 *
 * <p>Main responsibilities:
 * <ul>
 *   <li>Placing new orders</li>
 *   <li>Handling stock validation & rollback</li>
 *   <li>Assigning delivery partners</li>
 *   <li>Persisting orders and order items</li>
 *   <li>Syncing delivery updates</li>
 *   <li>Admin order status updates</li>
 *   <li>Fetching customer-specific orders</li>
 * </ul>
 *
 * <p>All financial values use {@link BigDecimal} with centralized scaling
 * and rounding configuration defined in {@link Constants}.
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
     * Places a new order following the complete business workflow.
     *
     * <p>Execution Flow:
     * <ol>
     *   <li>Validate that customer exists</li>
     *   <li>Reserve inventory from Restaurant Service</li>
     *   <li>Assign delivery partner via Delivery Service</li>
     *   <li>Persist order and order items</li>
     *   <li>Rollback inventory if any failure occurs</li>
     * </ol>
     *
     * <p>This method runs inside a transaction. If any step fails after
     * inventory reservation, a rollback attempt is made to release stock.
     *
     * @param request order placement request containing customer, restaurant,
     *                items, and delivery area details
     * @return saved {@link Order} entity
     * @throws InsufficientStockException if inventory validation fails
     * @throws OrderProcessingException if downstream service fails after reservation
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
     * Reserves menu items in the Restaurant Service and validates stock availability.
     *
     * <p>If reservation fails:
     * <ul>
     *   <li>Builds structured stock failure list</li>
     *   <li>Distinguishes between item-not-found and insufficient stock</li>
     *   <li>Throws {@link InsufficientStockException}</li>
     * </ul>
     *
     * @param request order placement request
     * @return successful {@link MenuReserveResponse}
     * @throws InsufficientStockException if reservation unsuccessful
     */
    private MenuReserveResponse reserveRestaurantInventory(PlaceOrderRequest request)
    {
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
     * Assigns a delivery partner via Delivery Service.
     *
     * <p>This single Feign call handles:
     * <ul>
     *   <li>Partner assignment</li>
     *   <li>Delivery charge calculation</li>
     *   <li>ETA estimation</li>
     * </ul>
     *
     * <p>If delivery service fails, a fallback response is generated
     * to allow order creation without blocking.
     *
     * @param orderId generated order ID
     * @param restaurantLocation source location
     * @param deliveryArea destination location
     * @return {@link DeliveryResponse} (real or fallback)
     */
    private DeliveryResponse assignDeliveryPartner(UUID orderId,
                                                   String restaurantLocation,
                                                   String deliveryArea)
    {
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
                log.info("Delivery partner assigned: {} for order: {}",
                        deliveryResp.getPartnerName(), orderId);
                return deliveryResp;
            } else {
                log.warn("Delivery service could not assign partner for order: {}. Using fallback.", orderId);
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
     * <p>Fallback values:
     * <ul>
     *   <li>Partner: SEARCHING_PARTNER</li>
     *   <li>Delivery charge: 0</li>
     *   <li>ETA: PENDING</li>
     *   <li>Status: false</li>
     * </ul>
     *
     * @return fallback {@link DeliveryResponse}
     */
    private DeliveryResponse createFallbackDeliveryResponse()
    {
        return new DeliveryResponse(
                Constants.SEARCHING_PARTNER,
                BigDecimal.ZERO,
                Constants.PENDING_ETA,
                false,
                Constants.DELIVERY_SERVICE_DELAY);
    }

    /**
     * Persists Order and corresponding OrderItem records.
     *
     * <p>Calculates:
     * <ul>
     *   <li>Subtotal from restaurant</li>
     *   <li>Delivery charge</li>
     *   <li>Total amount</li>
     * </ul>
     *
     * <p>Sets order status:
     * <ul>
     *   <li>PLACED if delivery partner assigned</li>
     *   <li>PREPARING if partner pending</li>
     * </ul>
     *
     * @param orderId unique order ID
     * @param request original request
     * @param reserveResp reservation response
     * @param deliveryResp delivery response
     * @return persisted {@link Order}
     */
    private Order saveOrderWithItems(UUID orderId,
                                     PlaceOrderRequest request,
                                     MenuReserveResponse reserveResp,
                                     DeliveryResponse deliveryResp)
    {
        log.debug("Saving order: {} with items", orderId);

        // Calculate totals
        BigDecimal subtotal = normalizeMoney(reserveResp.getSubtotal());
        BigDecimal deliveryCharge = normalizeMoney(deliveryResp.getDeliveryCharge());
        BigDecimal total = subtotal.add(deliveryCharge)
                .setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);

        // Create order entity (normalized: no customer/restaurant/delivery names; resolve via joins)
        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerId(request.getCustomerId());
        order.setRestaurantId(reserveResp.getRestaurantId());
        order.setFoodAmount(subtotal);
        order.setBeyond2kmCharge(deliveryCharge);
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
     * Releases reserved inventory back to Restaurant Service.
     *
     * <p>This is a best-effort rollback operation executed when order
     * placement fails after stock reservation.
     *
     * <p>Failure of this method does NOT rethrow exception.
     *
     * @param request original order request
     */
    private void releaseInventory(PlaceOrderRequest request)
    {
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
    private List<StockFailure> buildStockFailures(MenuReserveResponse reserveResp)
    {
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
     * Updates order status manually (Admin operation).
     *
     * <p>This method should be secured at controller layer
     * to allow only ADMIN role access.
     *
     * @param orderId target order ID
     * @param newStatus new {@link OrderStatus}
     * @return updated {@link Order}
     * @throws OrderNotFoundException if order not found
     */
    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus)
 {
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
     * Synchronizes order details from Delivery Service callback.
     *
     * <p>Supports partial updates:
     * <ul>
     *   <li>Delivery partner</li>
     *   <li>ETA</li>
     *   <li>Delivery charge (recalculates total)</li>
     *   <li>Status update</li>
     * </ul>
     *
     * <p>Automatically transitions PREPARING → PLACED
     * when partner gets assigned.
     *
     * @param syncReq delivery sync request
     * @return updated {@link Order}
     */
    @Transactional
    public Order syncOrder(OrderSyncRequest syncReq)
    {
        log.info("Syncing order: {}", syncReq.getOrderId());

        Order order = orderRepository.findById(syncReq.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        ErrorMessages.ORDER_NOT_FOUND_ID + syncReq.getOrderId()));

        boolean updated = false;

        if (syncReq.getDeliveryCharge() != null) {
            order.setBeyond2kmCharge(syncReq.getDeliveryCharge());
            // Recalculate total amount
            BigDecimal foodAmount = order.getFoodAmount() != null
                    ? order.getFoodAmount()
                    : BigDecimal.ZERO;
            order.setTotalAmount(foodAmount.add(syncReq.getDeliveryCharge()));
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
     * Fetches all orders for a specific customer,
     * sorted by newest first.
     *
     * @param customerId customer UUID
     * @return list of orders
     */
    public List<Order> getUserOrders(UUID customerId)
    {
        log.debug("getUserOrders customerId: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Fetches a single order for a specific customer.
     *
     * <p>Prevents users from accessing others' orders.
     *
     * @param orderId order UUID
     * @param customerId customer UUID
     * @return matching order
     * @throws OrderNotFoundException if not found or not owned by user
     */
    public Order getUserOrder(UUID orderId, UUID customerId)
    {
        log.debug("getUserOrder orderId: {} customerId: {}", orderId, customerId);
        return orderRepository.findByOrderIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Returns customer order summary with restaurant and delivery partner resolved via joins (normalized).
     *
     * @param customerId customer UUID
     * @return list of order summary DTOs with restaurant name and delivery partner from joined tables
     */
    public List<CustomerOrderSummaryDto> getCustomerOrderSummary(UUID customerId) {
        log.debug("getCustomerOrderSummary customerId: {}", customerId);
        List<Object[]> rows = orderRepository.findCustomerOrderSummaryByCustomerId(customerId);
        return rows.stream().map(this::mapRowToCustomerOrderSummary).toList();
    }

    /**
     * Maps a summary query row to {@link CustomerOrderSummaryDto}.
     *
     * @param row raw row from repository (orderId, customerId, totalAmount, status, createdAt,
     *            restaurantId, restaurantName, locationName, deliveryId, deliveryPartner, eta, deliveryStatus)
     * @return populated DTO
     */
    private CustomerOrderSummaryDto mapRowToCustomerOrderSummary(Object[] row) {
        CustomerOrderSummaryDto dto = new CustomerOrderSummaryDto();
        dto.setOrderId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
        dto.setCustomerId(row[1] != null ? UUID.fromString(row[1].toString()) : null);
        dto.setTotalAmount(row[2] != null ? new BigDecimal(row[2].toString()) : null);
        dto.setOrderStatus(row[3] != null ? OrderStatus.valueOf(row[3].toString()) : null);
        if (row[4] != null) {
            if (row[4] instanceof java.sql.Timestamp ts) {
                dto.setCreatedAt(ts.toInstant());
            } else if (row[4] instanceof Instant i) {
                dto.setCreatedAt(i);
            } else {
                dto.setCreatedAt(java.sql.Timestamp.valueOf(row[4].toString()).toInstant());
            }
        }
        dto.setRestaurantId(row[5] != null ? ((Number) row[5]).longValue() : null);
        dto.setRestaurantName(row[6] != null ? row[6].toString() : null);
        dto.setLocationName(row[7] != null ? row[7].toString() : null);
        dto.setDeliveryId(row[8] != null ? ((Number) row[8]).longValue() : null);
        dto.setDeliveryPartner(row[9] != null ? row[9].toString() : null);
        dto.setDeliveryEta(row[10] != null ? row[10].toString() : null);
        dto.setDeliveryStatus(row[11] != null ? row[11].toString() : null);
        return dto;
    }

    /**
     * Normalizes money values using configured scale and rounding.
     *
     * <p>If null, defaults to {@link BigDecimal#ZERO}.
     *
     * @param value monetary value
     * @return normalized amount
     */
    private BigDecimal normalizeMoney(BigDecimal value)
    {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
    }
}
