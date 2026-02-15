package com.foodapp.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MenuItemResponse {
    private Long menuItemId;
    private String itemName;
    private BigDecimal price;
    private boolean isVeg;
    private String category;
    private boolean available;

}
