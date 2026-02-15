package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationServiceTest {

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService();
    }

    @Test
    void getLatLng_knownArea_returnsCoords() {
        BigDecimal[] coords = locationService.getLatLng("Anna Nagar");
        assertThat(coords).hasSize(2);
        assertThat(coords[0].doubleValue()).isBetween(13.0, 14.0);
        assertThat(coords[1].doubleValue()).isBetween(80.0, 81.0);
    }

    @Test
    void getLatLng_caseInsensitive_returnsCoords() {
        BigDecimal[] coords = locationService.getLatLng("anna nagar");
        assertThat(coords).hasSize(2);
    }

    @Test
    void getLatLng_null_throws() {
        assertThatThrownBy(() -> locationService.getLatLng(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(Constants.LOCATION_REQUIRED);
    }

    @Test
    void getLatLng_unknownArea_throws() {
        assertThatThrownBy(() -> locationService.getLatLng("Unknown City"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(Constants.LOCATION_UNKNOWN);
    }

    @Test
    void getLatLng_trimmedInput_works() {
        BigDecimal[] coords = locationService.getLatLng("  Adyar  ");
        assertThat(coords).hasSize(2);
    }
}
