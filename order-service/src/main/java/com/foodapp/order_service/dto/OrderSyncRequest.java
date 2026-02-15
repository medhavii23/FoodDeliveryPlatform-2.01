package com.foodapp.order_service.dto;

import com.foodapp.order_service.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSyncRequest {
    private UUID orderId;
    private String partnerName;
    private String eta;
    private OrderStatus status;
    private BigDecimal deliveryCharge;
}
