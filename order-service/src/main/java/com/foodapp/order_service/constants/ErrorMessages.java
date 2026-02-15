package com.foodapp.order_service.constants;

public class ErrorMessages {
    private ErrorMessages() {
    }

    public static final String INSUFFICIENT_STOCK = "Insufficient stock for some items";
    public static final String ITEM_REQUIRED = "item required";
    public static final String INVALID_QTY = "qty is 0 or negative";
    public static final String ITEM_NOT_FOUND = "items doesn't exist";
    public static final String CUSTOMER_ID_REQUIRED = "Validation Error: Customer ID required for order placement";
    public static final String ORDER_FAILED_ROLLBACK = "Order failed after reserving stock. Inventory rollback triggered.";
    public static final String ORDER_NOT_FOUND_ID = "Order not found: ";
    public static final String CUSTOMER_ID_REQUIRED_VERIFY = "Validation Error: Customer ID required";
    public static final String DELIVERY_AREA_REQUIRED = "deliveryArea is required";
    public static final String UNKNOWN_DELIVERY_AREA = "Unknown deliveryArea: ";
}
