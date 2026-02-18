package com.foodapp.restaurant_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.restaurant_service.dto.ItemNameQty;
import com.foodapp.restaurant_service.dto.MenuReserveRequest;
import com.foodapp.restaurant_service.dto.MenuReserveResponse;
import com.foodapp.restaurant_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryController controller;


    @Test
    void testReserve() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        MenuReserveResponse resp = new MenuReserveResponse();
        when(inventoryService.reserve(any())).thenReturn(resp);

        mockMvc.perform(post("/api/restaurants/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testRelease() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        MenuReserveResponse resp = new MenuReserveResponse();
        when(inventoryService.release(any())).thenReturn(resp);

        mockMvc.perform(post("/api/restaurants/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testQuote() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);
        req.setItems(List.of(item));

        MenuReserveResponse resp = new MenuReserveResponse();
        when(inventoryService.quote(any())).thenReturn(resp);

        mockMvc.perform(post("/api/restaurants/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }


    @Test
    void testReserve_success() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");

        ItemNameQty item = new ItemNameQty();
        item.setItemName("Item 1");
        item.setQty(2);

        req.setItems(List.of(item));

        when(inventoryService.reserve(any())).thenReturn(new MenuReserveResponse());

        mockMvc.perform(post("/api/restaurants/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }


    @Test
    void testReserve_validationFailure() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        // restaurantName NOT set → invalid request

        mockMvc.perform(post("/api/restaurants/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReserve_itemsNull_validationFailure() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        req.setItems(null); // invalid by @NotEmpty

        mockMvc.perform(post("/api/restaurants/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testQuote_nullRestaurantName() throws Exception {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setItems(List.of());

        when(inventoryService.quote(any())).thenReturn(new MenuReserveResponse());

        mockMvc.perform(post("/api/restaurants/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testReserve_itemsNull_directCall() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        req.setItems(null);

        when(inventoryService.reserve(any()))
                .thenReturn(new MenuReserveResponse());

        MenuReserveResponse resp = controller.reserve(req);

        assertNotNull(resp);
    }

    @Test
    void testRelease_itemsNull_directCall() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        req.setItems(null);

        when(inventoryService.release(any()))
                .thenReturn(new MenuReserveResponse());

        MenuReserveResponse resp = controller.release(req);

        assertNotNull(resp);
    }

    @Test
    void testQuote_itemsNull_directCall() {
        MenuReserveRequest req = new MenuReserveRequest();
        req.setRestaurantName("Rest 1");
        req.setItems(null);

        when(inventoryService.quote(any()))
                .thenReturn(new MenuReserveResponse());

        MenuReserveResponse resp = controller.quote(req);

        assertNotNull(resp);
    }



}
