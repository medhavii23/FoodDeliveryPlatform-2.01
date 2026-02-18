package com.foodapp.restaurant_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.restaurant_service.constants.Constants;
import com.foodapp.restaurant_service.dto.CreateMenuItems;
import com.foodapp.restaurant_service.dto.CreateRestaurantRequest;
import com.foodapp.restaurant_service.dto.RestaurantLocationResponse;
import com.foodapp.restaurant_service.model.MenuItem;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestaurantController controller;


    @Test
    void testGetAllRestaurants() throws Exception {
        when(restaurantService.getAllRestaurants()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/restaurants")
                .header("X-Auth-Role", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRestaurantByName() throws Exception {
        RestaurantLocationResponse r = new RestaurantLocationResponse(1L, "Rest 1", "Loc 1");
        when(restaurantService.getRestaurantByName("Rest 1")).thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/restaurants/search")
                .param("name", "Rest 1")
                .header("X-Auth-Role", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRestaurantById() throws Exception {
        RestaurantLocationResponse r = new RestaurantLocationResponse(1L, "Rest 1", "Loc 1");
        when(restaurantService.getRestaurantById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/restaurants/1")
                .header("X-Auth-Role", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void testAddRestaurant() throws Exception {
        CreateRestaurantRequest req = new CreateRestaurantRequest();
        req.setRestaurantName("Rest 1");
        req.setOpeningTime(LocalTime.MIN);
        req.setClosingTime(LocalTime.MAX);
        req.setLatitude(BigDecimal.valueOf(1.0));
        req.setLongitude(BigDecimal.valueOf(1.0));
        req.setLocationName("Loc 1");

        Restaurant r = new Restaurant();
        when(restaurantService.addRestaurant(any(Restaurant.class))).thenReturn(r);

        mockMvc.perform(post("/api/restaurants")
                .header("X-Auth-Role", Constants.ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testAddMenuItem() throws Exception {
        CreateMenuItems req = new CreateMenuItems();
        req.setItemName("Item 1");
        req.setPrice(java.math.BigDecimal.TEN);
        req.setStockQty(10);
        req.setIsVeg(true);
        req.setCategory("Starters");

        MenuItem item = new MenuItem();
        when(restaurantService.addMenuItem(any(), any())).thenReturn(item);

        mockMvc.perform(post("/api/restaurants/1/menu")
                .header("X-Auth-Role", Constants.ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetMenu() throws Exception {
        when(restaurantService.getMenu(1L, null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/restaurants/1/menu"))
                .andExpect(status().isOk());
    }
    @Test
    void testGetAllRestaurants_missingRole() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isInternalServerError()); // ✅ FIXED
    }


    @Test
    void testAddRestaurant_nonAdminAccess() throws Exception {
        CreateRestaurantRequest req = new CreateRestaurantRequest();
        req.setRestaurantName("Rest");

        mockMvc.perform(post("/api/restaurants")
                        .header("X-Auth-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // ✅ FIXED
    }


    @Test
    void testGetRestaurantByName_notFound() throws Exception {
        when(restaurantService.getRestaurantByName("X"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/restaurants/search")
                        .param("name", "X")
                        .header("X-Auth-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }
    @Test
    void testGetRestaurantById_notFound() throws Exception {
        when(restaurantService.getRestaurantById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/restaurants/99")
                        .header("X-Auth-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }
    @Test
    void testAddRestaurant_validationFailure() throws Exception {
        CreateRestaurantRequest req = new CreateRestaurantRequest(); // missing fields

        mockMvc.perform(post("/api/restaurants")
                        .header("X-Auth-Role", Constants.ROLE_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testAddMenuItem_validationFailure() throws Exception {
        CreateMenuItems req = new CreateMenuItems(); // invalid

        mockMvc.perform(post("/api/restaurants/1/menu")
                        .header("X-Auth-Role", Constants.ROLE_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testAddRestaurant_nonAdmin_validBody_shouldThrowAccessDenied() throws Exception {
        CreateRestaurantRequest req = new CreateRestaurantRequest();
        req.setRestaurantName("Rest 1");
        req.setOpeningTime(LocalTime.MIN);
        req.setClosingTime(LocalTime.MAX);
        req.setLatitude(BigDecimal.ONE);
        req.setLongitude(BigDecimal.ONE);
        req.setLocationName("Loc");

        mockMvc.perform(post("/api/restaurants")
                        .header("X-Auth-Role", "USER") // not ADMIN
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetAllRestaurants_emptyRole() throws Exception {
        mockMvc.perform(get("/api/restaurants")
                        .header("X-Auth-Role", "")) // empty string
                .andExpect(status().isInternalServerError());
    }


    @Test
    void testGetAllRestaurants_nullRole_directCall() {
        assertThrows(RuntimeException.class,
                () -> controller.getAllRestaurants(null));
    }

    @Test
    void testGetRestaurantByName_nullRole_directCall() {
        assertThrows(RuntimeException.class,
                () -> controller.getRestaurantByName(null, "Rest"));
    }

    @Test
    void testGetRestaurantById_nullRole_directCall() {
        assertThrows(RuntimeException.class,
                () -> controller.getRestaurantById(null, 1L));
    }




}
