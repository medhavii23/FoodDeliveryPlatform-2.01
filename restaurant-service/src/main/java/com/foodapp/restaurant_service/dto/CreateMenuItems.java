package com.foodapp.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMenuItems {
    @jakarta.validation.constraints.NotBlank(message = "Item name is required")
    private String itemName;

    @jakarta.validation.constraints.NotNull(message = "Price is required")
    @jakarta.validation.constraints.Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @jakarta.validation.constraints.NotNull(message = "IsVeg flag is required")
    private Boolean isVeg;

    @jakarta.validation.constraints.NotBlank(message = "Category is required")
    private String category;

    @jakarta.validation.constraints.Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQty;

}
