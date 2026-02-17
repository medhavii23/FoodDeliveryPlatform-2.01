package com.foodapp.restaurant_service.service;

import com.foodapp.restaurant_service.constants.Constants;
import com.foodapp.restaurant_service.dto.*;
import com.foodapp.restaurant_service.exception.RestaurantNotFoundException;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.repository.MenuItemRepository;
import com.foodapp.restaurant_service.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for restaurant and menu CRUD operations.
 *
 * <p>Handles listing restaurants, lookup by name/id, adding restaurants and menu items,
 * and fetching menus with optional filters (isVeg, category).
 */
@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    /**
     * Checks if the restaurant is currently open based on opening/closing times.
     *
     * @param r restaurant entity
     * @return true if current time is within opening and closing time
     */
    public boolean isRestaurantOpen(Restaurant r) {
                LocalTime now = LocalTime.now();
                return !(now.isBefore(r.getOpeningTime()) || now.isAfter(r.getClosingTime()));
        }

    /**
     * Persists a new restaurant (ID is generated).
     *
     * @param r restaurant entity (restaurantId should be null)
     * @return saved restaurant with generated ID
     */
    public Restaurant addRestaurant(Restaurant r) {
        log.debug("addRestaurant: {}", r.getRestaurantName());
        r.setRestaurantId(null);
        return restaurantRepository.save(r);
    }

    /**
     * Returns all restaurants with id, name, and location name.
     *
     * @return list of restaurant location responses
     */
    public List<RestaurantLocationResponse> getAllRestaurants() {
        log.debug("getAllRestaurants");
        return restaurantRepository.findAll().stream()
                .map(r -> new RestaurantLocationResponse(
                        r.getRestaurantId(),
                        r.getRestaurantName(),
                        r.getLocationName()))
                .collect(Collectors.toList());
    }

    /**
     * Finds a restaurant by name (case-insensitive).
     *
     * @param name restaurant name
     * @return optional restaurant location response
     */
    public Optional<RestaurantLocationResponse> getRestaurantByName(String name) {
        log.debug("getRestaurantByName: {}", name);
        return restaurantRepository.findByRestaurantNameIgnoreCase(name)
                .map(r -> new RestaurantLocationResponse(
                        r.getRestaurantId(),
                        r.getRestaurantName(),
                        r.getLocationName()));
    }

    /**
     * Finds a restaurant by ID.
     *
     * @param id restaurant ID
     * @return optional restaurant location response
     */
    public Optional<RestaurantLocationResponse> getRestaurantById(Long id) {
        log.debug("getRestaurantById: {}", id);
        return restaurantRepository.findById(id)
                .map(r -> new RestaurantLocationResponse(
                        r.getRestaurantId(),
                        r.getRestaurantName(),
                        r.getLocationName()));
    }

    /**
     * Adds a menu item to a restaurant. Fails if an item with the same name already exists.
     *
     * @param restaurantId restaurant ID
     * @param item menu item to add
     * @return saved menu item
     * @throws RestaurantNotFoundException if restaurant not found
     * @throws RuntimeException if item name already exists for this restaurant
     */
    public MenuItem addMenuItem(Long restaurantId, MenuItem item) {
                Restaurant r = restaurantRepository.findById(restaurantId)
                                .orElseThrow(() -> RestaurantNotFoundException.byId(restaurantId));

        if (menuItemRepository.existsByRestaurantRestaurantIdAndItemNameIgnoreCase(restaurantId,
                item.getItemName())) {
            log.warn("MenuItem already exists for restaurant {}: {}", restaurantId, item.getItemName());
            throw new RuntimeException(
                    Constants.ITEM_ALREADY_EXISTS + item.getItemName());
        }

        log.info("Adding menu item to restaurant {}: {}", restaurantId, item.getItemName());
        item.setRestaurant(r);
        return menuItemRepository.save(item);
    }

    /**
     * Returns menu items for a restaurant, optionally filtered by vegetarian flag and category.
     *
     * @param restaurantId restaurant ID
     * @param isVeg optional filter for vegetarian items only
     * @param category optional filter by category name
     * @return list of menu item responses (includes availability)
     */
    public List<MenuItemResponse> getMenu(Long restaurantId, Boolean isVeg, String category) {
        log.debug("getMenu restaurantId: {} isVeg: {} category: {}", restaurantId, isVeg, category);
        List<MenuItem> items = menuItemRepository.findByRestaurantRestaurantId(restaurantId);

        return items.stream()
                                .filter(item -> {
                                        if (isVeg != null && !isVeg.equals(item.getIsVeg()))
                                                return false;
                                    return category == null || category.isEmpty() ||
                                            (item.getCategory() != null && item.getCategory()
                                                    .equalsIgnoreCase(category));
                                })
                                .map(menuItem -> new MenuItemResponse(
                                                menuItem.getMenuItemId(),
                                                menuItem.getItemName(),
                                                menuItem.getPrice(),
                                                menuItem.getIsVeg() != null && menuItem.getIsVeg(),
                                                menuItem.getCategory(),
                                                menuItem.isAvailable()))
                                .collect(Collectors.toList());
    }
}
