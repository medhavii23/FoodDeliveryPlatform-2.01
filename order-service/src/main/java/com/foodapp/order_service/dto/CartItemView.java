package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CartItemView {
    private String itemName;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}

