package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resolves area/location names to lat/lng coordinates for distance and charge calculation.
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private static final Map<String, BigDecimal[]> AREA = Map.of(
            "Anna Nagar", new BigDecimal[] { BigDecimal.valueOf(13.0850), BigDecimal.valueOf(80.2101) },
            "T Nagar", new BigDecimal[] { BigDecimal.valueOf(13.0418), BigDecimal.valueOf(80.2336) },
            "Velachery", new BigDecimal[] { BigDecimal.valueOf(12.9791), BigDecimal.valueOf(80.2214) },
            "Adyar", new BigDecimal[] { BigDecimal.valueOf(13.0012), BigDecimal.valueOf(80.2565) },
            "Mount Road", new BigDecimal[] { BigDecimal.valueOf(13.0694), BigDecimal.valueOf(80.2753) },
            "Besant Nagar", new BigDecimal[] { BigDecimal.valueOf(13.0003), BigDecimal.valueOf(80.2667) },
            "RA Puram", new BigDecimal[] { BigDecimal.valueOf(13.0268), BigDecimal.valueOf(80.2541) });

    /**
     * Returns [latitude, longitude] for a known area name (case-insensitive, supports "T. Nagar" / "T Nagar").
     *
     * @param area area/location name
     * @return array of [lat, lng]
     * @throws RuntimeException if area is null or unknown
     */
    public BigDecimal[] getLatLng(String area) {
        if (area == null) {
            log.debug("getLatLng: area is null");
            throw new RuntimeException(Constants.LOCATION_REQUIRED);
        }
        // Normalize: trim and match case-insensitive keys if needed, but Map is
        // case-sensitive.
        // For simplicity assuming exact match or we can improve to case-insensitive
        // loop.
        // Let's stick to exact match as per existing OrderService logic or user input.
        // Wait, "T. Nagar" vs "T Nagar"?
        // Detailed check: data.sql uses "T. Nagar", OrderService used "T Nagar".
        // I should probably support both or standardize.
        // Let's copy the keys I saw in data.sql + "T Nagar" just in case.

        // Actually, let's make it case-insensitive lookup
        for (Map.Entry<String, BigDecimal[]> entry : AREA.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(area.trim())) {
                return entry.getValue();
            }
            // Hande T. Nagar vs T Nagar
            String cleanKey = entry.getKey().replace(".", "").replace(" ", "");
            String cleanArea = area.replace(".", "").replace(" ", "");
            if (cleanKey.equalsIgnoreCase(cleanArea)) {
                return entry.getValue();
            }
        }

        log.warn("Unknown location: {}", area);
        throw new RuntimeException(Constants.LOCATION_UNKNOWN + area);
    }
}
