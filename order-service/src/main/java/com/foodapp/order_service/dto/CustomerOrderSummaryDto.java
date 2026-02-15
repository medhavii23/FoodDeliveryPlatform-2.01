package com.foodapp.order_service.dto;

import com.foodapp.order_service.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single order in the customer order summary (orders + restaurant + delivery via joins).
 * Normalized: restaurant name and delivery partner come from joined tables, not from order columns.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOrderSummaryDto {

    private UUID orderId;
    private UUID customerId;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private Instant createdAt;

    private Long restaurantId;
    private String restaurantName;
    private String locationName;

    private Long deliveryId;
    private String deliveryPartner;
    private String deliveryEta;
    private String deliveryStatus;
}
