package com.foodapp.order_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {
    private String partnerName;
    private BigDecimal deliveryCharge;
    private String eta;
    private boolean success;
    private String message;
}
