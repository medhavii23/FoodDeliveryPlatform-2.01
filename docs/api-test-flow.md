# API Test Flow – User & Admin

Reference for testing the full project flow via the API Gateway. All calls use **Base URL**: `http://localhost:8082` unless noted.

**Auth:** Register a user and/or admin, then call `POST /auth/token` with `username` and `password` to get a JWT. Use it as `Authorization: Bearer <token>`. The gateway validates the JWT and adds headers: `X-Auth-User`, `X-Auth-Role`, `X-Auth-Id`. Cart and order APIs use `X-Auth-Id` as the customer; no need to send these headers yourself when using the gateway.

---

## Auth Endpoints (no Bearer)

These paths do not require `Authorization`.

| Method | Path | Body | Notes |
|--------|------|------|--------|
| POST | `/auth/register` | `username` (string), `password` (string), `email` (string) | Creates user with role USER |
| POST | `/auth/register/admin` | Same as above | Creates user with role ADMIN |
| POST | `/auth/token` | `username` (string), `password` (string) | Returns JWT string |
| GET | `/auth/validate?token=<jwt>` | — | Query param: token. Returns "Token is valid" if OK |

**Example – Register user**

```bash
curl -X POST http://localhost:8082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pass1","email":"user1@example.com"}'
```

**Example – Register admin**

```bash
curl -X POST http://localhost:8082/auth/register/admin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"admin1","email":"admin1@example.com"}'
```

**Example – Get token**

```bash
curl -X POST http://localhost:8082/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pass1"}'
```

Save the returned JWT as `USER_TOKEN` or `ADMIN_TOKEN` and use in subsequent requests: `Authorization: Bearer <token>`.

---

## User Flow (test order)

Use a **user** token for these steps.

### 1. List all restaurants

| Method | Path | Headers | Body |
|--------|------|---------|------|
| GET | `/api/restaurants` | `Authorization: Bearer <USER_TOKEN>` | — |

### 2. Get restaurant by name

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/restaurants/search` | `Authorization: Bearer <USER_TOKEN>` | Query: `name` (e.g. `Saravana Bhavan`) |

Example: `GET /api/restaurants/search?name=Saravana%20Bhavan`

### 3. Get restaurant by ID

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/restaurants/{restaurantId}` | `Authorization: Bearer <USER_TOKEN>` | Path: `restaurantId` (long) |

### 4. Get menu

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/restaurants/{restaurantId}/menu` | `Authorization: Bearer <USER_TOKEN>` | Path: `restaurantId`. Optional query: `isVeg` (boolean), `category` (string) |

Example: `GET /api/restaurants/1/menu` or `GET /api/restaurants/1/menu?isVeg=true`

### 5. Add or update cart item

| Method | Path | Headers | Body |
|--------|------|---------|------|
| POST | `/api/cart/items` | `Authorization: Bearer <USER_TOKEN>`, `Content-Type: application/json` | `restaurantName` (string), `deliveryArea` (string), `itemName` (string), `qty` (integer, ≥ 0; 0 removes item) |

Example body (use a real restaurant and item from seed data):

```json
{
  "restaurantName": "Saravana Bhavan",
  "deliveryArea": "T Nagar",
  "itemName": "Mini Tiffin",
  "qty": 2
}
```

### 6. Get my cart

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/cart/my` | `Authorization: Bearer <USER_TOKEN>` | Optional query: `restaurantName` |

### 7. Checkout (create order)

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| POST | `/api/cart/checkout` | `Authorization: Bearer <USER_TOKEN>` | Optional query: `restaurantName` |

Returns the created order (includes `orderId`). Use this `orderId` for delivery assign and status updates.

### 8. My orders

| Method | Path | Headers | Body |
|--------|------|---------|------|
| GET | `/api/cart/orders` | `Authorization: Bearer <USER_TOKEN>` | — |

### 9. Get order by ID

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/cart/orders/{orderId}` | `Authorization: Bearer <USER_TOKEN>` | Path: `orderId` (UUID) |

### 10. Order summary (with restaurant and delivery partner)

| Method | Path | Headers | Body |
|--------|------|---------|------|
| GET | `/api/cart/orders/summary` | `Authorization: Bearer <USER_TOKEN>` | — |

### Optional: Clear my cart

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| DELETE | `/api/cart/my` | `Authorization: Bearer <USER_TOKEN>` | Optional query: `restaurantName` |

---

## Admin Flow (test order)

Use an **admin** token for these steps. All require `Authorization: Bearer <ADMIN_TOKEN>`.

### 1. Add restaurant

| Method | Path | Headers | Body |
|--------|------|---------|------|
| POST | `/api/restaurants` | `Authorization: Bearer <ADMIN_TOKEN>`, `Content-Type: application/json` | `restaurantName`, `openingTime` (HH:mm), `closingTime` (HH:mm), `latitude` (number), `longitude` (number), `locationName` |

Example body:

```json
{
  "restaurantName": "New Restaurant",
  "openingTime": "09:00",
  "closingTime": "22:00",
  "latitude": 13.0850,
  "longitude": 80.2101,
  "locationName": "Anna Nagar"
}
```

### 2. Add menu item

| Method | Path | Headers | Body |
|--------|------|---------|------|
| POST | `/api/restaurants/{restaurantId}/menu` | `Authorization: Bearer <ADMIN_TOKEN>`, `Content-Type: application/json` | `itemName`, `price` (number), `isVeg` (boolean), `category` (string), `stockQty` (integer, ≥ 0) |

Example body:

```json
{
  "itemName": "Paneer Tikka",
  "price": 199.00,
  "isVeg": true,
  "category": "Starters",
  "stockQty": 50
}
```

### 3. Estimate delivery (user or admin)

| Method | Path | Headers | Body |
|--------|------|---------|------|
| POST | `/api/delivery/estimate` | `Authorization: Bearer <ADMIN_TOKEN>`, `Content-Type: application/json` | `restaurantLocation` (string), `customerLocation` (string), `partnerName` (optional) |

Example body:

```json
{
  "restaurantLocation": "Anna Nagar",
  "customerLocation": "T Nagar"
}
```

### 4. Assign delivery partner (after an order exists)

| Method | Path | Headers | Body |
|--------|------|---------|------|
| POST | `/api/delivery/assign/{orderId}` | `Authorization: Bearer <ADMIN_TOKEN>`, `Content-Type: application/json` | Path: `orderId` (UUID). Body: `restaurantLocation`, `customerLocation`, `partnerName` (optional; omit for auto-assign) |

Example: `POST /api/delivery/assign/<orderId-from-checkout>` with same body as estimate.

### 5. Update delivery status

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| PUT | `/api/delivery/{orderId}/status` | `Authorization: Bearer <ADMIN_TOKEN>` | Path: `orderId`. Query: `status` = `ASSIGNED` \| `PICKED_UP` \| `OUT_FOR_DELIVERY` \| `DELIVERED` |

Example: `PUT /api/delivery/<orderId>/status?status=PICKED_UP`

### 6. Update order status

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| PUT | `/api/cart/orders/{orderId}/status` | `Authorization: Bearer <ADMIN_TOKEN>` | Path: `orderId`. Query: `status` = `PLACED` \| `PREPARING` \| `OUT_FOR_DELIVERY` \| `DELIVERED` |

Example: `PUT /api/cart/orders/<orderId>/status?status=OUT_FOR_DELIVERY`

### 7. Track delivery (user or admin)

| Method | Path | Headers | Params |
|--------|------|---------|--------|
| GET | `/api/delivery/{orderId}` | `Authorization: Bearer <ADMIN_TOKEN>` | Path: `orderId` |

---

## Role Summary

| Endpoint | Role |
|----------|------|
| `/auth/*` | No auth |
| `GET /api/restaurants`, `GET /api/restaurants/search`, `GET /api/restaurants/{id}`, `GET /api/restaurants/{id}/menu` | User or Admin |
| `POST /api/restaurants`, `POST /api/restaurants/{id}/menu` | Admin only |
| `POST /api/cart/items`, `GET /api/cart/my`, `DELETE /api/cart/my`, `POST /api/cart/checkout`, `GET /api/cart/orders`, `GET /api/cart/orders/{id}`, `GET /api/cart/orders/summary` | User or Admin |
| `PUT /api/cart/orders/{orderId}/status` | Admin only |
| `POST /api/delivery/estimate`, `GET /api/delivery/{orderId}` | User or Admin |
| `POST /api/delivery/assign/{orderId}`, `PUT /api/delivery/{orderId}/status` | Admin only |

---

## Internal / Sync (reference only)

- **POST** `/api/cart/orders/sync` – Used by delivery-service to push partner/ETA/charge/status to order-service. Body: `orderId`, `partnerName`, `eta`, `status`, `deliveryCharge`. Not needed for manual user/admin testing.

---

For a few ready-to-paste curl examples, see [docs/curl-examples.md](curl-examples.md). This document is the full ordered list of endpoints with all params and roles for testing the project flow.
