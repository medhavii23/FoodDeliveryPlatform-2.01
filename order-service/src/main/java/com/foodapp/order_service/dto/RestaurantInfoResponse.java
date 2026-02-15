package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantInfoResponse {
    private Long restaurantId;
    private String restaurantName;
    private String locationName;
}
