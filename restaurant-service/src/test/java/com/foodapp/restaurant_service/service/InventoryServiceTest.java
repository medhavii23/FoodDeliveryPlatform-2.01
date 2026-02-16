package com.foodapp.restaurant_service.service;

import com.foodapp.restaurant_service.dto.*;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.repository.MenuItemRepository;
import com.foodapp.restaurant_service.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Restaurant restaurant;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setRestaurantName("Test Rest");
        restaurant.setOpeningTime(LocalTime.MIN);
        restaurant.setClosingTime(LocalTime.MAX);

        menuItem = new MenuItem();
        menuItem.setItemName("Item 1");
        menuItem.setPrice(BigDecimal.TEN);
        menuItem.setStockQty(10);
        menuItem.setAvailable(true);
    }

    @Test
    void testQuote_Success() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertTrue(resp.isSuccess());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(resp.getSubtotal()));
    }

    @Test
    void testQuote_RestaurantClosed() {
        restaurant.setOpeningTime(LocalTime.MAX);
        restaurant.setClosingTime(LocalTime.MIN);
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(new ItemNameQty())); // dummy

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
        assertEquals("Restaurant is closed", resp.getMessage());
    }

    @Test
    void testReserve_Success() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getConfirmedItems().get(0).getQty());
    }

    @Test
    void testReserve_InsufficientStock() {
        menuItem.setStockQty(1); // Only 1 left

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2); // Want 2
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
        assertFalse(resp.getUnavailableItems().isEmpty());
    }

    @Test
    void testRelease_Success() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.release(req);

        assertTrue(resp.isSuccess());
        assertEquals("RELEASED", resp.getMessage());
    }
}
