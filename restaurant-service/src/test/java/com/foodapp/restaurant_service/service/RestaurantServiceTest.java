package com.foodapp.restaurant_service.service;

import com.foodapp.restaurant_service.dto.RestaurantLocationResponse;
import com.foodapp.restaurant_service.exception.RestaurantNotFoundException;
import com.foodapp.restaurant_service.model.Restaurant;
import com.foodapp.restaurant_service.repository.MenuItemRepository;
import com.foodapp.restaurant_service.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void getAllRestaurants_returnsMappedResponses() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(1L);
        r.setRestaurantName("Test Restaurant");
        r.setLocationName("Anna Nagar");
        when(restaurantRepository.findAll()).thenReturn(List.of(r));

        List<RestaurantLocationResponse> result = restaurantService.getAllRestaurants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestaurantName()).isEqualTo("Test Restaurant");
        assertThat(result.get(0).getLocationName()).isEqualTo("Anna Nagar");
    }

    @Test
    void getRestaurantByName_whenFound_returnsResponse() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(1L);
        r.setRestaurantName("Pizza Hub");
        r.setLocationName("T Nagar");
        when(restaurantRepository.findByRestaurantNameIgnoreCase("pizza hub")).thenReturn(Optional.of(r));

        Optional<RestaurantLocationResponse> opt = restaurantService.getRestaurantByName("pizza hub");

        assertThat(opt).isPresent();
        assertThat(opt.get().getRestaurantName()).isEqualTo("Pizza Hub");
    }

    @Test
    void getRestaurantByName_whenNotFound_returnsEmpty() {
        when(restaurantRepository.findByRestaurantNameIgnoreCase("unknown")).thenReturn(Optional.empty());
        assertThat(restaurantService.getRestaurantByName("unknown")).isEmpty();
    }

    @Test
    void addRestaurant_clearsIdAndSaves() {
        Restaurant r = new Restaurant();
        r.setRestaurantId(99L);
        r.setRestaurantName("New Place");
        r.setLocationName("Adyar");
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> {
            Restaurant saved = i.getArgument(0);
            saved.setRestaurantId(1L);
            return saved;
        });

        Restaurant result = restaurantService.addRestaurant(r);

        assertThat(result.getRestaurantId()).isEqualTo(1L);
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void isRestaurantOpen_whenWithinHours_returnsTrue() {
        Restaurant r = new Restaurant();
        r.setOpeningTime(LocalTime.of(10, 0));
        r.setClosingTime(LocalTime.of(22, 0));
        assertThat(restaurantService.isRestaurantOpen(r)).isTrue();
    }
}
