package com.foodapp.restaurant_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class MenuReserveResponse {
    private Boolean success;
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
