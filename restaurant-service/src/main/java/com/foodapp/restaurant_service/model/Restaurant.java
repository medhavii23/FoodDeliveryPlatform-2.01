package com.foodapp.restaurant_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;

    private String restaurantName;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String locationName;
}
