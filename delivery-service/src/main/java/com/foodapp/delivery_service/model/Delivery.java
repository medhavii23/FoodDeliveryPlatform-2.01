package com.foodapp.delivery_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    private UUID orderId;
    private String partnerName;
    private BigDecimal deliveryCharge;
    private String eta;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
}
