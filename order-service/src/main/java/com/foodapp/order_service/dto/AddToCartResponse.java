package com.foodapp.order_service.dto;

import com.foodapp.order_service.model.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartResponse {

    private UUID cartId;
    private UUID customerId;
    private String customerName;
    private String restaurantName;
    private String deliveryArea;
    private CartStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CartItemView> items;
    private BigDecimal subtotal;
    private BigDecimal total;
}
