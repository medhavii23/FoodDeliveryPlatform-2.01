package com.foodapp.restaurant_service.service;

import com.foodapp.restaurant_service.dto.*;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.repository.MenuItemRepository;
import com.foodapp.restaurant_service.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for menu inventory operations: quote (pricing only), reserve (deduct stock),
 * and release (add stock back on rollback).
 *
 * <p>Uses row-level locking on reserve/release to avoid race conditions.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;


    private boolean isRestaurantOpen(Restaurant r) {
        LocalTime now = LocalTime.now();
        return !(now.isBefore(r.getOpeningTime()) || now.isAfter(r.getClosingTime()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isInvalidQty(Integer qty) {
        return qty == null || qty <= 0;
    }


    private MenuReserveResponse invalidResponse(String message) {
        return new MenuReserveResponse(false, message,
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, List.of(), List.of());
    }

    private MenuReserveResponse responseRestaurantNotFound(String restaurantName) {
        return new MenuReserveResponse(false, "Restaurant not found: " + restaurantName,
                null, restaurantName,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, List.of(), List.of());
    }

    private MenuReserveResponse responseRestaurantClosed(Restaurant restaurant) {
        return new MenuReserveResponse(false, "Restaurant is closed",
                restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                restaurant.getLatitude(), restaurant.getLongitude(), restaurant.getLocationName(),
                BigDecimal.ZERO, List.of(), List.of());
    }

    private MenuReserveResponse responseItemsUnavailable(Restaurant restaurant, List<UnavailableItem> unavailable) {
        return new MenuReserveResponse(false, "One or more items unavailable",
                restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                restaurant.getLatitude(), restaurant.getLongitude(), restaurant.getLocationName(),
                BigDecimal.ZERO, List.of(), unavailable);
    }

    private MenuReserveResponse responseQuoted(Restaurant restaurant, BigDecimal subtotal, List<ConfirmedItem> confirmed) {
        return new MenuReserveResponse(true, "QUOTED",
                restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                restaurant.getLatitude(), restaurant.getLongitude(), restaurant.getLocationName(),
                subtotal, confirmed, List.of());
    }

    private MenuReserveResponse responseReserved(Restaurant restaurant, BigDecimal subtotal, List<ConfirmedItem> confirmed) {
        return new MenuReserveResponse(true, "RESERVED",
                restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                restaurant.getLatitude(), restaurant.getLongitude(), restaurant.getLocationName(),
                subtotal, confirmed, List.of());
    }

    private MenuReserveResponse responseReleased(Restaurant restaurant, List<ConfirmedItem> confirmed) {
        return new MenuReserveResponse(true, "RELEASED",
                restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                restaurant.getLatitude(), restaurant.getLongitude(), restaurant.getLocationName(),
                BigDecimal.ZERO, confirmed, List.of());
    }


    private MenuReserveResponse validateRequest(MenuReserveRequest req) {
        if (req == null) return invalidResponse("request body is required");
        if (isBlank(req.getRestaurantName())) return invalidResponse("restaurantName is required");
        if (req.getItems() == null || req.getItems().isEmpty()) return invalidResponse("items are required");
        return null;
    }

    private record ValidationResult(boolean valid, String itemName, int requestedQty, String message) {
        static ValidationResult ok(String name, int qty) {
            return new ValidationResult(true, name, qty, null);
        }

        static ValidationResult fail(String message) {
            return new ValidationResult(false, null, 0, message);
        }
    }

    private ValidationResult validateItemInput(ItemNameQty it) {
        if (it == null) return ValidationResult.fail("item required");

        String name = safeTrim(it.getItemName());
        if (name.isEmpty()) return ValidationResult.fail("item required");

        Integer qty = it.getQty();
        if (isInvalidQty(qty)) return ValidationResult.fail("qty is 0 or negative");

        return ValidationResult.ok(name, qty);
    }

    /**
     * For quote/reserve: checks if the item exists in menu, is marked available, and has enough stock.
     * Returns an UnavailableItem reason if it cannot be processed; otherwise returns null.
     */
    private UnavailableItem validateReservable(MenuItem dbItem, int want) {
        if (!dbItem.isAvailable()) {
            return new UnavailableItem(dbItem.getMenuItemId(), dbItem.getItemName(), want, dbItem.getStockQty());
        }
        int have = dbItem.getStockQty();
        if (have < want) {
            return new UnavailableItem(dbItem.getMenuItemId(), dbItem.getItemName(), want, have);
        }
        return null;
    }

    /**
     * Fetch restaurant by name; returns null if not found.
     */
    private Restaurant fetchRestaurant(String restaurantName) {
        return restaurantRepository.findByRestaurantNameIgnoreCase(restaurantName.trim()).orElse(null);
    }



    /**
     * Quote: returns item prices and subtotal without reserving/deducting stock.
     * Used for cart view to show unit prices and subtotal.
     *
     * @param req restaurant name and items with quantities
     * @return quote response (success with confirmed items and subtotal, or failure with message/unavailable items)
     */
    public MenuReserveResponse quote(MenuReserveRequest req) {

        MenuReserveResponse invalid = validateRequest(req);
        if (invalid != null) return invalid;

        log.debug("quote restaurant: {}", req.getRestaurantName());


        Restaurant restaurant = fetchRestaurant(req.getRestaurantName());
        if (restaurant == null) {
            log.warn("Quote failed: restaurant not found: {}", req.getRestaurantName());
            return responseRestaurantNotFound(req.getRestaurantName());
        }

        if (!isRestaurantOpen(restaurant)) return responseRestaurantClosed(restaurant);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<ConfirmedItem> confirmed = new ArrayList<>();
        List<UnavailableItem> unavailable = new ArrayList<>();

        for (ItemNameQty it : req.getItems()) {

            ValidationResult v = validateItemInput(it);
            if (!v.valid()) {
                unavailable.add(new UnavailableItem(null, v.message(), v.requestedQty(), 0));
                continue;
            }

            String itemName = v.itemName();
            int want = v.requestedQty();

            MenuItem dbItem = menuItemRepository
                    .findByRestaurantNameAndItemName(restaurant.getRestaurantName(), itemName)
                    .orElse(null);

            if (dbItem == null) {
                unavailable.add(new UnavailableItem(null, "item doesn't exist: " + itemName, want, 0));
                continue;
            }

            UnavailableItem reason = validateReservable(dbItem, want);
            if (reason != null) {
                unavailable.add(reason);
                continue;
            }

            BigDecimal lineTotal = dbItem.getPrice().multiply(BigDecimal.valueOf(want));
            subtotal = subtotal.add(lineTotal);

            confirmed.add(new ConfirmedItem(
                    dbItem.getMenuItemId(),
                    dbItem.getItemName(),
                    dbItem.getPrice(),
                    want,
                    lineTotal,
                    dbItem.getStockQty()
            ));
        }

        if (!unavailable.isEmpty()) {
            return responseItemsUnavailable(restaurant, unavailable);
        }

        return responseQuoted(restaurant, subtotal, confirmed);
    }


    /**
     * Reserve: validates all items and then deducts stock atomically.
     * Uses row-level locking (FOR UPDATE) to avoid race conditions.
     *
     * @param req restaurant name and items with quantities
     * @return reserve response (success with confirmed items and subtotal, or failure)
     */
    @Transactional
    public MenuReserveResponse reserve(MenuReserveRequest req) {

        MenuReserveResponse invalid = validateRequest(req);
        if (invalid != null) return invalid;
        log.info("reserve restaurant: {} items: {}", req.getRestaurantName(), req.getItems() != null ? req.getItems().size() : 0);


        Restaurant restaurant = fetchRestaurant(req.getRestaurantName());
        if (restaurant == null) {
            log.warn("Reserve failed: restaurant not found: {}", req.getRestaurantName());
            return responseRestaurantNotFound(req.getRestaurantName());
        }

        if (!isRestaurantOpen(restaurant)) {
            log.warn("Reserve failed: restaurant closed: {}", req.getRestaurantName());
            return responseRestaurantClosed(restaurant);
        }

        // Validate ALL items first with row locks; deduct only after all are valid
        List<UnavailableItem> unavailable = new ArrayList<>();
        List<MenuItem> lockedItems = new ArrayList<>();
        List<Integer> lockedQty = new ArrayList<>();

        for (ItemNameQty it : req.getItems()) {

            ValidationResult v = validateItemInput(it);
            if (!v.valid()) {
                unavailable.add(new UnavailableItem(null, v.message(), v.requestedQty(), 0));
                continue;
            }

            String itemName = v.itemName();
            int want = v.requestedQty();

            MenuItem dbItem = menuItemRepository
                    .findForUpdateByRestaurantNameAndItemName(restaurant.getRestaurantName(), itemName)
                    .orElse(null);

            if (dbItem == null) {
                unavailable.add(new UnavailableItem(null, "item doesn't exist: " + itemName, want, 0));
                continue;
            }

            UnavailableItem reason = validateReservable(dbItem, want);
            if (reason != null) {
                unavailable.add(reason);
                continue;
            }

            lockedItems.add(dbItem);
            lockedQty.add(want);
        }

        if (!unavailable.isEmpty()) {
            return responseItemsUnavailable(restaurant, unavailable);
        }

        // Deduct stock + compute subtotal + build confirmed items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<ConfirmedItem> confirmed = new ArrayList<>();

        for (int i = 0; i < lockedItems.size(); i++) {
            MenuItem dbItem = lockedItems.get(i);
            int want = lockedQty.get(i);

            dbItem.setStockQty(dbItem.getStockQty() - want);

            BigDecimal lineTotal = dbItem.getPrice().multiply(BigDecimal.valueOf(want));
            subtotal = subtotal.add(lineTotal);

            confirmed.add(new ConfirmedItem(
                    dbItem.getMenuItemId(),
                    dbItem.getItemName(),
                    dbItem.getPrice(),
                    want,
                    lineTotal,
                    dbItem.getStockQty()
            ));
        }

        log.info("Reserve successful for restaurant: {} subtotal: {}", req.getRestaurantName(), subtotal);
        return responseReserved(restaurant, subtotal, confirmed);
    }



    /**
     * Release: add stock back (best-effort) for the given items.
     * Used when order fails after reserve.
     *
     * <p>Behavior:
     * <ul>
     *   <li>Skips invalid item inputs (blank name / qty <= 0)</li>
     *   <li>Skips items not found</li>
     *   <li>Locks rows to avoid concurrent updates</li>
     * </ul>
     */
    @Transactional
    public MenuReserveResponse release(MenuReserveRequest req) {

        MenuReserveResponse invalid = validateRequest(req);
        if (invalid != null) return invalid;

        log.info("release restaurant: {} items: {}", req.getRestaurantName(), req.getItems() != null ? req.getItems().size() : 0);



        Restaurant restaurant = fetchRestaurant(req.getRestaurantName());
        if (restaurant == null) {
            log.warn("Release failed: restaurant not found: {}", req.getRestaurantName());
            return responseRestaurantNotFound(req.getRestaurantName());
        }

        List<ConfirmedItem> confirmed = new ArrayList<>();

        for (ItemNameQty it : req.getItems()) {

            ValidationResult v = validateItemInput(it);
            if (!v.valid()) {
                // For release, we skip invalid inputs (best effort cleanup)
                continue;
            }

            String itemName = v.itemName();
            int addBack = v.requestedQty();

            MenuItem dbItem = menuItemRepository
                    .findForUpdateByRestaurantNameAndItemName(restaurant.getRestaurantName(), itemName)
                    .orElse(null);

            if (dbItem == null) {
                // Best effort: skip unknown items
                continue;
            }

            dbItem.setStockQty(dbItem.getStockQty() + addBack);

            BigDecimal releasedTotal = dbItem.getPrice().multiply(BigDecimal.valueOf(addBack));

            confirmed.add(new ConfirmedItem(
                    dbItem.getMenuItemId(),
                    dbItem.getItemName(),
                    dbItem.getPrice(),
                    addBack,
                    releasedTotal,
                    dbItem.getStockQty()
            ));
        }

        return responseReleased(restaurant, confirmed);
    }
}
