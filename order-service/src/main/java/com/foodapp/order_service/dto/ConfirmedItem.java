package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedItem {
    private Long itemId;
    private String itemName;
    private BigDecimal unitPrice;
    private int qty;
    private BigDecimal lineTotal;
    private int remainingStock;

}
