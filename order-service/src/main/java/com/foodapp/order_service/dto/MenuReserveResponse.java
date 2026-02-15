package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuReserveResponse {
    private boolean success;
    private String message;

    private Long restaurantId;
    private String restaurantName;

    private BigDecimal restaurantLat;
    private BigDecimal restaurantLng;
    private String locationName;

    private BigDecimal subtotal;
    private List<ConfirmedItem> confirmedItems;

    private List<UnavailableItem> unavailableItems;
}
