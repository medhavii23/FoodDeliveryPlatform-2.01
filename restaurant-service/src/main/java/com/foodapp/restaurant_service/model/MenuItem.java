package com.foodapp.restaurant_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor
public class MenuItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuItemId;

    private String itemName;
    private BigDecimal price;

    private int stockQty;

    private Boolean isVeg;
    private String category; //STARTER, MAIN, DESSERT, DRINK

    private boolean available=true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable=false)
    private Restaurant restaurant;

}
