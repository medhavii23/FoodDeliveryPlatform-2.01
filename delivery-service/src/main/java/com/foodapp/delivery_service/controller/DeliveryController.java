package com.foodapp.delivery_service.controller;

import com.foodapp.delivery_service.constants.Constants;
import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
import com.foodapp.delivery_service.model.Delivery;
import com.foodapp.delivery_service.model.DeliveryStatus;
import com.foodapp.delivery_service.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * REST controller for delivery: estimate, assign partner, track, and update
 * status.
 */
@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private DeliveryService deliveryService;

    /**
     * Estimates delivery charge and ETA from restaurant and customer locations (no
     * persistence).
     *
     * @param role    caller role (user or admin required)
     * @param request restaurant and customer location names
     * @return delivery response with charge and ETA
     */
    @PostMapping("/estimate")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public DeliveryResponse estimateDelivery(
            @RequestBody @jakarta.validation.Valid DeliveryAssignRequest request) {
        log.info("estimateDelivery from {} to {}", request.getRestaurantLocation(), request.getCustomerLocation());
        return deliveryService.estimateDelivery(request);
    }

    /**
     * Assigns a delivery partner to an order (admin only). Persists delivery and
     * syncs to Order Service.
     *
     * @param role    caller role (must be admin)
     * @param orderId order UUID
     * @param request restaurant/customer locations and optional partner name
     * @return delivery response with partner, charge, ETA
     */
    @PostMapping("/assign/{orderId}")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DeliveryResponse assignDelivery(@PathVariable UUID orderId,
            @RequestBody @jakarta.validation.Valid DeliveryAssignRequest request) {
        log.info("assignDelivery orderId: {}", orderId);
        return deliveryService.assignDeliveryPartner(orderId, request);
    }

    /**
     * Returns delivery record for an order (tracking).
     *
     * @param role    caller role (user or admin required)
     * @param orderId order UUID
     * @return delivery entity
     */
    @GetMapping("/{orderId}")
    public Delivery track(@PathVariable UUID orderId) {
        log.debug("track orderId: {}", orderId);
        return deliveryService.track(orderId);
    }

    /**
     * Updates delivery status (admin only).
     *
     * @param role    caller role (must be admin)
     * @param orderId order UUID
     * @param status  new delivery status
     * @return updated delivery entity
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Delivery updateStatus(
            @PathVariable UUID orderId,
            @RequestParam DeliveryStatus status) {
        log.info("updateStatus orderId: {} status: {}", orderId, status);
        return deliveryService.updateStatus(orderId, status);
    }
}
