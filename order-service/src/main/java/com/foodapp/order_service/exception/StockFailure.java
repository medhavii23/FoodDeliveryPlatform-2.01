package com.foodapp.order_service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockFailure {
    private Long itemId;
    private String itemName;
    private int requested;
    private int available;
}
