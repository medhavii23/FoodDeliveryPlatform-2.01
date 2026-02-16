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
}
