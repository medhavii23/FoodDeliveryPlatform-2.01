-- Delivery schema: delivery records per order
CREATE SCHEMA IF NOT EXISTS delivery;

CREATE TABLE delivery.deliveries (
    delivery_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    order_id UUID NOT NULL,
    partner_name VARCHAR(255),
    delivery_charge DECIMAL(19, 2),
    eta VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_deliveries_order
        FOREIGN KEY (order_id) REFERENCES order_schema.orders(order_id)
);
