package com.foodapp.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemNameQty {

    @NotBlank(message = "Item name is required")
    @Schema(description = "Name of the menu item", example = "Idly")
    private String itemName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the item", example = "2")
    private Integer qty;
}
