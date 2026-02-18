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
    @Test
    void testQuote_NullRequest() {
        MenuReserveResponse resp = inventoryService.quote(null);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_BlankRestaurantName() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName(" ");
        req.setItems(List.of(new ItemNameQty()));

        MenuReserveResponse resp = inventoryService.quote(req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_EmptyItems() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(Collections.emptyList());

        MenuReserveResponse resp = inventoryService.quote(req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_RestaurantNotFound() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Unknown");
        req.setItems(List.of(new ItemNameQty()));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Restaurant not found"));
    }
    @Test
    void testQuote_InvalidItem_NullItem() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");

        List<ItemNameQty> items = new java.util.ArrayList<>();
        items.add(null);   // allowed here
        req.setItems(items);

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_InvalidItem_BlankName() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName(" ");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testQuote_InvalidQty() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(0);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testQuote_ItemDoesNotExist() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Unknown");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testQuote_ItemNotAvailable() {
        menuItem.setAvailable(false);

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testRelease_SkipInvalidItem() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName(" ");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.release(req);

        assertTrue(resp.isSuccess());
    }
    @Test
    void testRelease_ItemNotFound() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Unknown");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.release(req);

        assertTrue(resp.isSuccess());
    }

    @Test
    void testReserve_RestaurantNotFound() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Unknown");
        req.setItems(List.of(new ItemNameQty()));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testReserve_RestaurantClosed() {
        restaurant.setOpeningTime(LocalTime.MAX);
        restaurant.setClosingTime(LocalTime.MIN);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(new ItemNameQty()));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
        assertEquals("Restaurant is closed", resp.getMessage());
    }
    @Test
    void testReserve_ItemDoesNotExist() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Unknown");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testReserve_ItemNotAvailable() {
        menuItem.setAvailable(false);

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testRelease_RestaurantNotFound() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Unknown");
        req.setItems(List.of(new ItemNameQty()));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.release(req);

        assertFalse(resp.isSuccess());
    }
    @Test
    void testQuote_InvalidQty_Null() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(null); // ← important

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_ItemNameNull() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName(null); // ← important
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }


    @Test
    void testQuote_ItemsNull() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(null); // ← missing branch

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_InsufficientStock() {
        menuItem.setStockQty(1); // only 1 available

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(5); // want more than available

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testReserve_InvalidItemInput() {
        ItemNameQty item = new ItemNameQty();
        item.setItemName(" "); // invalid
        item.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    void testQuote_ExactlyAtOpeningTime() {
        restaurant.setOpeningTime(LocalTime.now());
        restaurant.setClosingTime(LocalTime.MAX);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(1);

        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertTrue(resp.isSuccess());
    }

    @Test
    void testQuote_ExactStockMatch() {
        menuItem.setStockQty(5);

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(5);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(item));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.quote(req);

        assertTrue(resp.isSuccess());
    }

    @Test
    void testReserve_MultipleItems() {
        MenuItem second = new MenuItem();
        second.setItemName("Item 2");
        second.setPrice(BigDecimal.TEN);
        second.setStockQty(10);
        second.setAvailable(true);

        ItemNameQty i1 = new ItemNameQty();
        i1.setItemName("Item 1");
        i1.setQty(1);

        ItemNameQty i2 = new ItemNameQty();
        i2.setItemName("Item 2");
        i2.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(i1, i2));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));
        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 2"))
                .thenReturn(Optional.of(second));

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertTrue(resp.isSuccess());
    }

    @Test
    void testRelease_NullRequest() {
        MenuReserveResponse resp = inventoryService.release(null);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testRelease_BlankRestaurantName() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName(" ");
        req.setItems(List.of(new ItemNameQty()));

        MenuReserveResponse resp = inventoryService.release(req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testRelease_ItemsNull() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(null);

        MenuReserveResponse resp = inventoryService.release(req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testReserve_NullRequest() {
        MenuReserveResponse resp = inventoryService.reserve(null);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testReserve_BlankRestaurantName() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName(" ");
        req.setItems(List.of(new ItemNameQty()));

        MenuReserveResponse resp = inventoryService.reserve(req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void testReserve_ItemsNull() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(null);

        MenuReserveResponse resp = inventoryService.reserve(req);
        assertFalse(resp.isSuccess());
    }
    @Test
    void testQuote_MixedValidAndInvalidItems() {

        ItemNameQty valid = new ItemNameQty();
        valid.setItemName("Item 1");
        valid.setQty(1);

        ItemNameQty invalid = new ItemNameQty();
        invalid.setItemName("Unknown");
        invalid.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(valid, invalid));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        when(menuItemRepository.findByRestaurantNameAndItemName("Test Rest", "Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.quote(req);

        assertFalse(resp.isSuccess());
        assertFalse(resp.getUnavailableItems().isEmpty());
    }

    @Test
    void testReserve_MixedValidAndInvalidItems() {

        ItemNameQty valid = new ItemNameQty();
        valid.setItemName("Item 1");
        valid.setQty(1);

        ItemNameQty invalid = new ItemNameQty();
        invalid.setItemName("Unknown");
        invalid.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(valid, invalid));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Unknown"))
                .thenReturn(Optional.empty());

        MenuReserveResponse resp = inventoryService.reserve(req);

        assertFalse(resp.isSuccess());
        assertFalse(resp.getUnavailableItems().isEmpty());
    }

    @Test
    void testRelease_MixedItems() {

        ItemNameQty valid = new ItemNameQty();
        valid.setItemName("Item 1");
        valid.setQty(2);

        ItemNameQty invalid = new ItemNameQty();
        invalid.setItemName(" ");
        invalid.setQty(1);

        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Test Rest");
        req.setItems(List.of(valid, invalid));

        when(restaurantRepository.findByRestaurantNameIgnoreCase("Test Rest"))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findForUpdateByRestaurantNameAndItemName("Test Rest", "Item 1"))
                .thenReturn(Optional.of(menuItem));

        MenuReserveResponse resp = inventoryService.release(req);

        assertTrue(resp.isSuccess());
        assertEquals(1, resp.getConfirmedItems().size());
    }




}
