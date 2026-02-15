package com.foodapp.order_service.dto;

import lombok.Data;

@Data
public class CartItemUpdateRequest {
    @jakarta.validation.constraints.NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    @jakarta.validation.constraints.NotBlank(message = "Delivery area is required")
    private String deliveryArea;

    @jakarta.validation.constraints.NotBlank(message = "Item name is required")
    private String itemName;

    @jakarta.validation.constraints.NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.Min(value = 0, message = "Quantity cannot be negative")
    private Integer qty;
}
