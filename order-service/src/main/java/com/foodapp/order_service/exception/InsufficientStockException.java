package com.foodapp.order_service.exception;

import java.util.List;

public class InsufficientStockException extends RuntimeException{
    private final List<StockFailure> failures;

    public InsufficientStockException(String message, List<StockFailure> failures) {
        super(message);
        this.failures = failures;
    }

    public List<StockFailure> getFailures() {
        return failures;
    }
}
