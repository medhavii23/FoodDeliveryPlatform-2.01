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

import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


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

    // -------------------------
    // Happy paths (already had)
    // -------------------------

    @Test
    void testEstimateDelivery_created() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);
        when(deliveryService.estimateDelivery(any())).thenReturn(new DeliveryResponse());

        mockMvc.perform(post("/api/delivery/estimate")
                        .header(Constants.AUTH_ROLE, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testAssignDelivery_created_admin() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner 1");
        when(deliveryService.assignDeliveryPartner(any(), any())).thenReturn(new DeliveryResponse());

        mockMvc.perform(post("/api/delivery/assign/" + UUID.randomUUID())
                        .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void testTrack_ok() throws Exception {
        when(deliveryService.track(any())).thenReturn(new Delivery());

        mockMvc.perform(get("/api/delivery/" + UUID.randomUUID())
                        .header(Constants.AUTH_ROLE, "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateStatus_ok_admin() throws Exception {
        when(deliveryService.updateStatus(any(), eq(DeliveryStatus.DELIVERED))).thenReturn(new Delivery());

        mockMvc.perform(put("/api/delivery/" + UUID.randomUUID() + "/status")
                        .param("status", "DELIVERED")
                        .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN))
                .andExpect(status().isOk());
    }

    // -------------------------
    // Validation / parse errors
    // -------------------------

    @Test
    void testEstimateDelivery_validationFail_400() throws Exception {
        // DTO has @NotBlank, so blanks -> 400
        DeliveryAssignRequest invalid = new DeliveryAssignRequest("   ", "   ", null);

        mockMvc.perform(post("/api/delivery/estimate")
                        .header(Constants.AUTH_ROLE, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAssignDelivery_validationFail_400() throws Exception {
        // invalid restaurantLocation triggers @Valid -> 400
        DeliveryAssignRequest invalid = new DeliveryAssignRequest("", "Loc B", "Partner 1");

        mockMvc.perform(post("/api/delivery/assign/" + UUID.randomUUID())
                        .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEstimateDelivery_malformedJson_400() throws Exception {
        mockMvc.perform(post("/api/delivery/estimate")
                        .header(Constants.AUTH_ROLE, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateStatus_invalidEnum_shouldReturn500_dueToGlobalHandler() throws Exception {
        mockMvc.perform(put("/api/delivery/" + UUID.randomUUID() + "/status")
                        .param("status", "NOT_A_STATUS")
                        .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testEstimateDelivery_missingRoleHeader_shouldReturn500_dueToGlobalHandler() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        mockMvc.perform(post("/api/delivery/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void cover_checkUserOrAdmin_roleNull_branch_viaReflection() throws Exception {
        DeliveryController controller = new DeliveryController();

        Method m = DeliveryController.class.getDeclaredMethod("checkUserOrAdmin", String.class);
        m.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                m.invoke(controller, new Object[]{ null });
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw (RuntimeException) e.getCause(); // unwrap
            }
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining(Constants.ACCESS_DENIED_MISSING_ROLE);
    }

    @Test
    void cover_checkAdmin_adminBranch_viaReflection() throws Exception {
        DeliveryController controller = new DeliveryController();

        Method m = DeliveryController.class.getDeclaredMethod("checkAdmin", String.class);
        m.setAccessible(true);

        // Should NOT throw for ADMIN (covers the "if condition false" branch directly)
        m.invoke(controller, Constants.ROLE_ADMIN);
    }



    // -------------------------
    // Branch coverage for role checks
    // -------------------------

    @Test
    void testEstimateDelivery_emptyRole_500() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        mockMvc.perform(post("/api/delivery/estimate")
                        .header(Constants.AUTH_ROLE, "") // checkUserOrAdmin -> throws RuntimeException
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testTrack_emptyRole_500() throws Exception {
        mockMvc.perform(get("/api/delivery/" + UUID.randomUUID())
                        .header(Constants.AUTH_ROLE, "")) // checkUserOrAdmin -> throws
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testAssignDelivery_notAdmin_500() throws Exception {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner 1");

        mockMvc.perform(post("/api/delivery/assign/" + UUID.randomUUID())
                        .header(Constants.AUTH_ROLE, "USER") // checkAdmin -> throws RuntimeException
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateStatus_notAdmin_500() throws Exception {
        mockMvc.perform(put("/api/delivery/" + UUID.randomUUID() + "/status")
                        .param("status", "DELIVERED")
                        .header(Constants.AUTH_ROLE, "USER")) // checkAdmin -> throws
                .andExpect(status().isInternalServerError());
    }


}
