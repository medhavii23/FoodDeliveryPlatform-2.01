# Food Delivery Platform — Database Schema

Single shared H2 database with four schemas: **identity**, **restaurant**, **order_schema**, **delivery**. Tables are normalized (no redundant names on orders/carts); names and delivery info are resolved via JOINs. DDL is managed by Flyway migrations in **h2-server** (run h2-server first so migrations apply before other services start).

## Schemas

| Schema        | Purpose |
|---------------|---------|
| **identity**  | Users (customers); source of truth for user id, username, email, role. |
| **restaurant**| Restaurants and menu items. |
| **order_schema** | Customers (link to identity), orders, order_items, carts, cart_items. |
| **delivery**  | Delivery records per order (partner, ETA, status). |

## Cross-schema relationships (FKs)

- **order_schema.customers.customer_id** → identity.app_users(id)
- **order_schema.orders.customer_id** → identity.app_users(id)
- **order_schema.orders.restaurant_id** → restaurant.restaurants(restaurant_id)
- **order_schema.order_items.order_id** → order_schema.orders(order_id)
- **order_schema.order_items.menu_item_id** → restaurant.menu_items(menu_item_id)
- **order_schema.carts.customer_id** → identity.app_users(id)
- **order_schema.carts.restaurant_id** → restaurant.restaurants(restaurant_id)
- **order_schema.cart_items.cart_id** → order_schema.carts(cart_id)
- **delivery.deliveries.order_id** → order_schema.orders(order_id)

## Single-query customer order summary

For a given customer, one query can return all their orders with restaurant name and delivery partner (no redundant columns on orders):

- **Endpoint**: `GET /api/cart/orders/summary` (with `X-Auth-Id` = customer UUID).
- **Query**: JOINs `order_schema.orders` + `restaurant.restaurants` + `delivery.deliveries`; filters by `customer_id`.

## ER diagram (Mermaid)

```mermaid
erDiagram
    identity_app_users {
        uuid id PK
        string username
        string email
        string password
        string role
    }

    restaurant_restaurants {
        long restaurant_id PK
        string restaurant_name
        time opening_time
        time closing_time
        decimal latitude
        decimal longitude
        string location_name
    }

    restaurant_menu_items {
        long menu_item_id PK
        long restaurant_id FK
        string item_name
        decimal price
        int stock_qty
        boolean is_veg
        string category
        boolean available
    }

    order_schema_customers {
        uuid customer_id PK_FK
    }

    order_schema_orders {
        uuid order_id PK
        uuid customer_id FK
        long restaurant_id FK
        decimal food_amount
        decimal beyond2km_charge
        decimal total_amount
        string status
        instant created_at
    }

    order_schema_order_items {
        long id PK
        uuid order_id FK
        long menu_item_id FK
        string item_name
        decimal unit_price
        int qty
        decimal line_total
    }

    order_schema_carts {
        uuid cart_id PK
        uuid customer_id FK
        long restaurant_id FK
        string delivery_area
        uuid order_id
        string status
        instant created_at
        instant updated_at
    }

    order_schema_cart_items {
        long id PK
        uuid cart_id FK
        string item_name
        int qty
    }

    delivery_deliveries {
        long delivery_id PK
        uuid order_id FK
        string partner_name
        decimal delivery_charge
        string eta
        string status
    }

    identity_app_users ||--o| order_schema_customers : customer_id
    identity_app_users ||--o| order_schema_orders : customer_id
    identity_app_users ||--o| order_schema_carts : customer_id
    restaurant_restaurants ||--o| restaurant_menu_items : restaurant_id
    restaurant_restaurants ||--o| order_schema_orders : restaurant_id
    restaurant_restaurants ||--o| order_schema_carts : restaurant_id
    restaurant_menu_items ||--o| order_schema_order_items : menu_item_id
    order_schema_orders ||--o| order_schema_order_items : order_id
    order_schema_carts ||--o| order_schema_cart_items : cart_id
    order_schema_orders ||--o| delivery_deliveries : order_id
```
