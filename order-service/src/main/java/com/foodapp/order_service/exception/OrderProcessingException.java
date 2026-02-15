package com.foodapp.order_service.exception;

/**
 * Thrown when order placement fails after inventory reservation (e.g. delivery service failure).
 * Indicates that rollback (inventory release) was attempted.
 */
public class OrderProcessingException extends RuntimeException {
    /**
     * @param message error message (e.g. rollback notice)
     * @param cause the underlying exception
     */
    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
