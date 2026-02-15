package com.foodapp.restaurant_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemNameQty {
    @NotBlank private String itemName;
    @NotNull @Min
   (1) private Integer qty;
}
