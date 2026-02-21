package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.constants.Constants;
import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
import com.foodapp.delivery_service.dto.OrderSyncRequest;
import com.foodapp.delivery_service.exception.DeliveryNotFoundException;
import com.foodapp.delivery_service.feign.OrderClient;
import com.foodapp.delivery_service.model.Delivery;
import com.foodapp.delivery_service.model.DeliveryStatus;
import com.foodapp.delivery_service.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for delivery estimation, partner assignment, tracking, and status
 * updates.
 * Syncs assignment to Order Service via Feign.
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private PartnerNameGenerator partnerNameGenerator;

    /**
     * Calculates delivery charge and ETA from restaurant and customer locations.
     * Does NOT persist anything or assign a partner.
     *
     * @param req restaurant and customer location names
     * @return response with delivery charge and ETA
     */
    public DeliveryResponse estimateDelivery(DeliveryAssignRequest req) {
        log.debug("estimateDelivery from {} to {}", req.getRestaurantLocation(), req.getCustomerLocation());
        BigDecimal deliveryCharge;
        String eta = Constants.NA;

        String rLoc = req.getRestaurantLocation();
        String cLoc = req.getCustomerLocation();

        if (rLoc != null && cLoc != null) {
            try {
                BigDecimal[] rCoords = locationService.getLatLng(rLoc);
                BigDecimal[] cCoords = locationService.getLatLng(cLoc);

                validateCoords(rCoords[0], rCoords[1], "restaurant");
                validateCoords(cCoords[0], cCoords[1], "customer");

                double distanceInKm = calculateDistance(
                        rCoords[0].doubleValue(), rCoords[1].doubleValue(),
                        cCoords[0].doubleValue(), cCoords[1].doubleValue());

                if (distanceInKm > Constants.FREE_DELIVERY_DISTANCE) {
                    double charge = Math
                            .ceil((distanceInKm - Constants.FREE_DELIVERY_DISTANCE) * Constants.CHARGE_PER_KM);
                    deliveryCharge = BigDecimal.valueOf(charge).setScale(Constants.MONEY_SCALE,
                            Constants.MONEY_ROUNDING);
                } else {
                    deliveryCharge = BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
                }

                int et = (int) Math.ceil(distanceInKm * Constants.TIME_PER_KM);
                eta = et + " mins";
            } catch (Exception _) {
                deliveryCharge = BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
            }
        } else {
            deliveryCharge = BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING);
        }

        return new DeliveryResponse(
                null,
                deliveryCharge,
                eta,
                true,
                Constants.ASSIGN_SUCCESS);
    }

    /**
     * Assigns a delivery partner. When partnerName is null, attempts
     * auto-assignment
     * (~90% success). When explicitly provided (manual admin assign), always uses
     * that.
     * Persists delivery and syncs to Order Service.
     *
     * @param orderId order UUID
     * @param req     locations and optional partner name
     * @return delivery response with partner, charge, ETA
     */
    public DeliveryResponse assignDeliveryPartner(UUID orderId, DeliveryAssignRequest req) {
        log.info("assignDeliveryPartner orderId: {}", orderId);

        Optional<Delivery> existing = deliveryRepository.findByOrderId(orderId);

        // ALWAYS calculate delivery charge & ETA first (Order Service needs this)
        DeliveryResponse estimate = estimateDelivery(req);
        BigDecimal deliveryCharge = estimate.getDeliveryCharge();
        String eta = estimate.getEta();

        String partnerName = req.getPartnerName();

        // If not provided, try to auto-assign
        if (partnerName == null || partnerName.isBlank()) {
            // 90% chance to get a partner, 10% chance null (simulated failure)
            partnerName = partnerNameGenerator.randomPartnerName();
        }

        Delivery delivery = existing.orElse(new Delivery());
        delivery.setOrderId(orderId);
        delivery.setPartnerName(partnerName); // Can be null
        delivery.setDeliveryCharge(deliveryCharge);
        delivery.setEta(eta);

        boolean isAssigned = (partnerName != null);
        if (isAssigned) {
            delivery.setStatus(DeliveryStatus.ASSIGNED);
        } else {
            delivery.setStatus(DeliveryStatus.PENDING);
            log.warn("Delivery partner not found (10% fail case) for order: {}. Setting status to PENDING.", orderId);
        }

        deliveryRepository.save(delivery);

        // Synchronous Sync Push to Order Service (even if pending, to update valid
        // charge)
        try {
            orderClient.syncOrder(new OrderSyncRequest(orderId, partnerName, eta, deliveryCharge));
            log.debug("Order sync sent for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to sync order to Order Service: {}", orderId, e);
        }

        if (isAssigned) {
            return new DeliveryResponse(
                    partnerName,
                    deliveryCharge,
                    eta,
                    true,
                    Constants.ASSIGN_SUCCESS);
        } else {
            // Return success=true so Order Service proceeds with "PREPARING" order,
            // but indicate pending manual assignment via message/null partner.
            return new DeliveryResponse(
                    null,
                    deliveryCharge,
                    eta,
                    true,
                    "Pending manual assignment");
        }

    }

    /**
     * Returns delivery record for an order.
     *
     * @param orderId order UUID
     * @return delivery entity
     * @throws DeliveryNotFoundException if no delivery for order
     */
    public Delivery track(UUID orderId) {
        log.debug("track orderId: {}", orderId);
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException(orderId));
    }

    /**
     * Updates delivery status for an order.
     *
     * @param orderId order UUID
     * @param status  new status
     * @return updated delivery entity
     */
    public Delivery updateStatus(UUID orderId, DeliveryStatus status) {
        log.info("updateStatus orderId: {} status: {}", orderId, status);
        Delivery delivery = track(orderId);
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private void validateCoords(BigDecimal lat, BigDecimal lon, String type) {
        if (lat == null || lon == null) {
            throw new RuntimeException(Constants.COORDINATES_MISSING + type);
        }
    }

}
