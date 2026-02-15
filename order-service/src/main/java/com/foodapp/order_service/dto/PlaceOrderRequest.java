package com.foodapp.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Unique identifier for the customer", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID customerId;

    @NotBlank(message = "Customer name is required")
    @Schema(description = "Name of the customer", example = "John Doe")
    private String customerName;

    @NotBlank(message = "Restaurant name is required")
    @Schema(description = "Name of the restaurant", example = "Saravana Bhavan")
    private String restaurantName;

    @NotBlank(message = "Delivery area is required")
    @Schema(description = "Delivery area/location", example = "Koramangala")
    private String deliveryArea;

    @NotEmpty(message = "At least one item is required")
    @Valid
    @Schema(description = "List of items to order")
    private List<ItemNameQty> items;
}
