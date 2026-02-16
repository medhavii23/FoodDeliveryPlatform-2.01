package com.foodapp.restaurant_service.service;

import com.foodapp.restaurant_service.constants.Constants;
import com.foodapp.restaurant_service.dto.RestaurantLocationResponse;
import com.foodapp.restaurant_service.dto.MenuItemResponse;
import com.foodapp.restaurant_service.exception.RestaurantNotFoundException;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.repository.MenuItemRepository;
import com.foodapp.restaurant_service.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void testIsRestaurantOpen() {
        Restaurant r = new Restaurant();
        r.setOpeningTime(LocalTime.MIN);
        r.setClosingTime(LocalTime.MAX);
        assertTrue(restaurantService.isRestaurantOpen(r));

        r.setOpeningTime(LocalTime.MAX);
        r.setClosingTime(LocalTime.MIN);
        assertFalse(restaurantService.isRestaurantOpen(r));
    }

    @Test
    void testAddRestaurant() {
        Restaurant r = new Restaurant();
        r.setRestaurantName("Test Rest");
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(r);

        Restaurant saved = restaurantService.addRestaurant(r);
        assertNotNull(saved);
        assertEquals("Test Rest", saved.getRestaurantName());
    }

    @Test
    void testGetAllRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(Collections.emptyList());
        List<RestaurantLocationResponse> list = restaurantService.getAllRestaurants();
        assertTrue(list.isEmpty());
    }

    @Test
    void testGetRestaurantByName() {
        Restaurant r = new Restaurant();
        r.setRestaurantName("Test Rest");
        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest")).thenReturn(Optional.of(r));

        Optional<RestaurantLocationResponse> res = restaurantService.getRestaurantByName("Test Rest");
        assertTrue(res.isPresent());
        assertEquals("Test Rest", res.get().getRestaurantName());
    }

    @Test
    void testGetRestaurantById() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(r));

        Optional<RestaurantLocationResponse> res = restaurantService.getRestaurantById(1L);
        assertTrue(res.isPresent());
        assertEquals(1L, res.get().getRestaurantId());
    }

    @Test
    void testAddMenuItem_Success() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(1L);
        MenuItem item = new MenuItem();
        item.setItemName("Item 1");

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(r));
        when(menuItemRepository.existsByRestaurantRestaurantIdAndItemNameIgnoreCase(1L, "Item 1")).thenReturn(false);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(item);

        MenuItem saved = restaurantService.addMenuItem(1L, item);
        assertNotNull(saved);
    }

    @Test
    void testAddMenuItem_RestaurantNotFound() {
        MenuItem item = new MenuItem();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RestaurantNotFoundException.class, () -> restaurantService.addMenuItem(1L, item));
    }

    @Test
    void testAddMenuItem_Exists() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(1L);
        MenuItem item = new MenuItem();
        item.setItemName("Item 1");

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(r));
        when(menuItemRepository.existsByRestaurantRestaurantIdAndItemNameIgnoreCase(1L, "Item 1")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> restaurantService.addMenuItem(1L, item));
    }

    @Test
    void testGetMenu() {
        MenuItem item = new MenuItem();
        item.setItemName("Item 1");
        item.setAvailable(true);
        item.setIsVeg(true);
        item.setCategory("Starters");

        when(menuItemRepository.findByRestaurantRestaurantId(1L)).thenReturn(List.of(item));

        List<MenuItemResponse> menu = restaurantService.getMenu(1L, null, null);
        assertFalse(menu.isEmpty());

        menu = restaurantService.getMenu(1L, true, "Starters");
        assertFalse(menu.isEmpty());

        menu = restaurantService.getMenu(1L, false, null);
        assertTrue(menu.isEmpty());
    }
}
