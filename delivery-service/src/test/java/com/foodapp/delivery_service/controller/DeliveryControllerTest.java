package com.foodapp.delivery_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.delivery_service.constants.Constants;
import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
import com.foodapp.delivery_service.model.Delivery;
import com.foodapp.delivery_service.model.DeliveryStatus;
import com.foodapp.delivery_service.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEstimateDelivery() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);
        DeliveryResponse resp = new DeliveryResponse();
        when(deliveryService.estimateDelivery(any())).thenReturn(resp);

        mockMvc.perform(post("/api/delivery/estimate")
                .header(Constants.AUTH_ROLE, "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testAssignDelivery() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner 1");
        DeliveryResponse resp = new DeliveryResponse();
        when(deliveryService.assignDeliveryPartner(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/delivery/assign/" + UUID.randomUUID())
                .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testTrack() throws Exception {
        Delivery delivery = new Delivery();
        when(deliveryService.track(any())).thenReturn(delivery);

        mockMvc.perform(get("/api/delivery/" + UUID.randomUUID())
                .header(Constants.AUTH_ROLE, "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateStatus() throws Exception {
        Delivery delivery = new Delivery();
        when(deliveryService.updateStatus(any(), eq(DeliveryStatus.DELIVERED))).thenReturn(delivery);

        mockMvc.perform(put("/api/delivery/" + UUID.randomUUID() + "/status")
                .param("status", "DELIVERED")
                .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN))
                .andExpect(status().isOk());
    }
}
