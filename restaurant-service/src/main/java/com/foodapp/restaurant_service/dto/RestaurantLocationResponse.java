package com.foodapp.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantLocationResponse {
    private Long restaurantId;
    private String restaurantName;
    private String locationName;

}
