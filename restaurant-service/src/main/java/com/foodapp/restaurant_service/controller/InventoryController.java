package com.foodapp.restaurant_service.controller;

import com.foodapp.restaurant_service.dto.MenuReserveRequest;
import com.foodapp.restaurant_service.dto.MenuReserveResponse;
import com.foodapp.restaurant_service.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for inventory operations: quote, reserve, and release.
 *
 * <p>Used by Order Service for cart pricing (quote) and order placement (reserve/release).
 */
@RestController
@RequestMapping("/api/restaurants")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryService inventoryService;

    /**
     * Reserves menu items and deducts stock atomically (for order placement).
     *
     * @param req restaurant name and list of item name + quantity
     * @return reserve response (success/failure, confirmed items, unavailable items)
     */
    @PostMapping("/reserve")
    public MenuReserveResponse reserve(@Valid @RequestBody MenuReserveRequest req) {
        log.info("reserve restaurant: {} items: {}", req.getRestaurantName(), req.getItems() != null ? req.getItems().size() : 0);
        return inventoryService.reserve(req);
    }

    /**
     * Releases previously reserved stock (rollback when order fails).
     *
     * @param req restaurant name and list of item name + quantity to add back
     * @return release response
     */
    @PostMapping("/release")
    public MenuReserveResponse release(@RequestBody MenuReserveRequest req) {
        log.info("release restaurant: {} items: {}", req.getRestaurantName(), req.getItems() != null ? req.getItems().size() : 0);
        return inventoryService.release(req);
    }

    /**
     * Returns item prices and subtotal without reserving (for cart view).
     *
     * @param req restaurant name and list of item name + quantity
     * @return quote response with confirmed items and subtotal
     */
    @PostMapping("/quote")
    public MenuReserveResponse quote(@RequestBody MenuReserveRequest req) {
        log.debug("quote restaurant: {}", req.getRestaurantName());
        return inventoryService.quote(req);
    }
}