package com.foodapp.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnavailableItem {
    private Long itemId;
    private String itemName;
    private Integer requestedQty;
    private Integer availableQty;
}
