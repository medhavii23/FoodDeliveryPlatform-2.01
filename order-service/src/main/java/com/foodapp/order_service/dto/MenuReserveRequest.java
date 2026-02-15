package com.foodapp.order_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuReserveRequest {
    private String restaurantName;
    private List<ItemNameQty> items;
}
