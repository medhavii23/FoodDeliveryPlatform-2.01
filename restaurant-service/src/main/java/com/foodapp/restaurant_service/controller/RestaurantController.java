package com.foodapp.restaurant_service.controller;

import com.foodapp.restaurant_service.dto.*;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.service.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for restaurant and menu operations.
 *
 * <p>
 * Exposes endpoints for listing restaurants, searching by name/id,
 * adding restaurants and menu items (admin), and fetching restaurant menus.
 */
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);

    @Autowired
    private RestaurantService restaurantService;

    /**
     * Returns all restaurants with id, name, and location.
     *
     * @return list of restaurant location responses
     */
    @GetMapping
    public List<RestaurantLocationResponse> getAllRestaurants() {
        log.debug("getAllRestaurants");
        return restaurantService.getAllRestaurants();
    }

    /**
     * Returns a restaurant by exact name match (case-insensitive).
     *
     * @param name restaurant name
     * @return restaurant location response
     */
    @GetMapping("/search")
    public RestaurantLocationResponse getRestaurantByName(@RequestParam String name) {
        log.info("getRestaurantByName: {}", name);
        return restaurantService.getRestaurantByName(name)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + name));
    }

    /**
     * Returns a restaurant by ID.
     *

     * @param restaurantId restaurant ID
     * @return restaurant location response
     */
    @GetMapping("/{restaurantId}")
    public RestaurantLocationResponse getRestaurantById(@PathVariable Long restaurantId) {
        log.debug("getRestaurantById: {}", restaurantId);
        return restaurantService.getRestaurantById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));
    }

    /**
     * Creates a new restaurant (admin only).
     *
     * @param restaurant create request with name, times, location
     * @return saved restaurant entity
     */
    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Restaurant addRestaurant(@RequestBody @jakarta.validation.Valid CreateRestaurantRequest restaurant) {
        Restaurant r = new Restaurant();
        r.setRestaurantName(restaurant.getRestaurantName());
        r.setOpeningTime(restaurant.getOpeningTime());
        r.setClosingTime(restaurant.getClosingTime());
        r.setLatitude(restaurant.getLatitude());
        r.setLongitude(restaurant.getLongitude());
        r.setLocationName(restaurant.getLocationName());

        log.info("addRestaurant: {}", restaurant.getRestaurantName());
        return restaurantService.addRestaurant(r);
    }

    /**
     * Adds a menu item to a restaurant (admin only).
     *
     * @param restaurantId restaurant ID
     * @param item         menu item (name, price, stock, isVeg, category)
     * @return saved menu item
     */
    @PostMapping("/{restaurantId}/menu")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MenuItem addMenuItem(@PathVariable Long restaurantId,
            @RequestBody @jakarta.validation.Valid CreateMenuItems item) {
        MenuItem i = new MenuItem();
        i.setItemName(item.getItemName());
        i.setPrice(item.getPrice());
        i.setStockQty(item.getStockQty());
        i.setIsVeg(item.getIsVeg());
        i.setCategory(item.getCategory());
        log.info("addMenuItem restaurantId: {} item: {}", restaurantId, item.getItemName());
        return restaurantService.addMenuItem(restaurantId, i);
    }

    /**
     * Returns menu items for a restaurant, optionally filtered by isVeg and
     * category.
     *
     * @param restaurantId restaurant ID
     * @param isVeg        optional filter for vegetarian items
     * @param category     optional filter by category
     * @return list of menu item responses
     */
    @GetMapping("/{restaurantId}/menu")
    public List<MenuItemResponse> getMenu(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Boolean isVeg,
            @RequestParam(required = false) String category) {
        log.debug("getMenu restaurantId: {} isVeg: {} category: {}", restaurantId, isVeg, category);
        return restaurantService.getMenu(restaurantId, isVeg, category);
    }

}
