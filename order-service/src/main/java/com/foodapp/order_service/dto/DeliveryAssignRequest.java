package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignRequest {
    private String restaurantLocation;
    private String customerLocation;
    private String partnerName;
}
