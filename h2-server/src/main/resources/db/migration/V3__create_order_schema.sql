-- Order schema (normalized): customers, orders, order_items, carts, cart_items
-- No redundant name/partner columns; resolve via joins to identity and restaurant
CREATE SCHEMA IF NOT EXISTS order_schema;

CREATE TABLE order_schema.customers (
    customer_id UUID NOT NULL PRIMARY KEY,
    CONSTRAINT fk_customers_user
        FOREIGN KEY (customer_id) REFERENCES identity.app_users(id)
);

CREATE TABLE order_schema.orders (
    order_id UUID NOT NULL PRIMARY KEY,
    customer_id UUID NOT NULL,
    restaurant_id BIGINT NOT NULL,
    food_amount DECIMAL(19, 2) NOT NULL,
    beyond2km_charge DECIMAL(19, 2),
    total_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES identity.app_users(id),
    CONSTRAINT fk_orders_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant.restaurants(restaurant_id)
);

CREATE TABLE order_schema.order_items (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    order_id UUID NOT NULL,
    menu_item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    qty INT NOT NULL,
    line_total DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES order_schema.orders(order_id),
    CONSTRAINT fk_order_items_menu_item
        FOREIGN KEY (menu_item_id) REFERENCES restaurant.menu_items(menu_item_id)
);

CREATE TABLE order_schema.carts (
    cart_id UUID NOT NULL PRIMARY KEY,
    customer_id UUID NOT NULL,
    restaurant_id BIGINT NOT NULL,
    delivery_area VARCHAR(255),
    order_id UUID,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_carts_customer
        FOREIGN KEY (customer_id) REFERENCES identity.app_users(id),
    CONSTRAINT fk_carts_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant.restaurants(restaurant_id)
);

CREATE TABLE order_schema.cart_items (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    cart_id UUID NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    qty INT NOT NULL,
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES order_schema.carts(cart_id)
);
