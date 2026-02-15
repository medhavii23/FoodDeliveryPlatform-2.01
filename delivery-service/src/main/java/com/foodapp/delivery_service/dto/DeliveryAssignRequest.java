package com.foodapp.delivery_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignRequest {
    @jakarta.validation.constraints.NotBlank(message = "Restaurant location is required")
    private String restaurantLocation;

    @jakarta.validation.constraints.NotBlank(message = "Customer location is required")
    private String customerLocation;

    private String partnerName;
}
