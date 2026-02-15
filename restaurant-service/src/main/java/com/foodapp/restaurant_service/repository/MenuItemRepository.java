package com.foodapp.restaurant_service.repository;

import com.foodapp.restaurant_service.model.MenuItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

        List<MenuItem> findByRestaurantRestaurantId(Long restaurantId);

        boolean existsByRestaurantRestaurantIdAndItemNameIgnoreCase(Long restaurantId, String itemName);

        @Query("""
                select mi from MenuItem mi
                join mi.restaurant r
                where lower(r.restaurantName) = lower(:restaurantName)
                  and lower(mi.itemName) = lower(:itemName)
            """)
        Optional<MenuItem> findByRestaurantNameAndItemName(
                @Param("restaurantName") String restaurantName,
                @Param("itemName") String itemName);

        // Pessimistic lock so two users can't oversell stock
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            select mi from MenuItem mi
                            join mi.restaurant r
                            where lower(r.restaurantName) = lower(:restaurantName)
                              and lower(mi.itemName) = lower(:itemName)
                        """)
        Optional<MenuItem> findForUpdateByRestaurantNameAndItemName(
                        @Param("restaurantName") String restaurantName,
                        @Param("itemName") String itemName);

        Optional<MenuItem> findByMenuItemIdAndRestaurantRestaurantId(Long menuItemId, Long restaurantId);
}
