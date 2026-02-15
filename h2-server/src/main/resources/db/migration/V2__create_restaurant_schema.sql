-- Restaurant schema: restaurants and menu items
CREATE SCHEMA IF NOT EXISTS restaurant;

CREATE TABLE restaurant.restaurants (
    restaurant_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    restaurant_name VARCHAR(255) NOT NULL,
    opening_time TIME,
    closing_time TIME,
    latitude DECIMAL(19, 6),
    longitude DECIMAL(19, 6),
    location_name VARCHAR(255)
);

CREATE TABLE restaurant.menu_items (
    menu_item_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    stock_qty INT NOT NULL,
    is_veg BOOLEAN,
    category VARCHAR(50),
    available BOOLEAN DEFAULT TRUE,
    restaurant_id BIGINT NOT NULL,
    CONSTRAINT fk_menu_items_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant.restaurants(restaurant_id)
);
