package com.foodapp.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRestaurantRequest {
    @jakarta.validation.constraints.NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    @jakarta.validation.constraints.NotNull(message = "Opening time is required")
    private LocalTime openingTime;

    @jakarta.validation.constraints.NotNull(message = "Closing time is required")
    private LocalTime closingTime;

    @jakarta.validation.constraints.NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @jakarta.validation.constraints.NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @jakarta.validation.constraints.NotBlank(message = "Location name is required")
    private String locationName;
}
