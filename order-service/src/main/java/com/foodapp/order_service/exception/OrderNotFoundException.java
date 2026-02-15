package com.foodapp.order_service.exception;

import java.util.UUID;

/**
 * Thrown when an order is not found by ID or when the order does not belong to the requesting customer.
 */
public class OrderNotFoundException extends RuntimeException {
    /**
     * @param orderId the order ID that was not found
     */
    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
