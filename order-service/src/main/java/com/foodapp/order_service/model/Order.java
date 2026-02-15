package com.foodapp.order_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    private UUID customerId;
    @Id
    private UUID orderId;
    private Long restaurantId;

    private BigDecimal foodAmount;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private BigDecimal beyond2kmCharge;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Instant createdAt;
}
