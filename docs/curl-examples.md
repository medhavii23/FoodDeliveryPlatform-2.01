# API examples (API Gateway on port 8082)

Base URL: `http://localhost:8082`

## 1. Register

```bash
curl --request POST \
  --url http://localhost:8082/auth/register \
  --header 'content-type: application/json' \
  --data '{
  "username": "karav",
  "password": "yaya",
  "email": "yaya@comcast.com"
}'
```

## 2. Get token

```bash
curl --request POST \
  --url http://localhost:8082/auth/token \
  --header 'content-type: application/json' \
  --data '{
  "username": "karav",
  "password": "yaya"
}'
```

Copy the JWT from the response and use it as `YOUR_JWT_TOKEN` below.

## 3. Add item to cart

Use a real menu item name from seed data (e.g. `Mini Tiffin`, `Ghee Roast Dosa`).

```bash
curl --request POST \
  --url http://localhost:8082/api/cart/items \
  --header 'authorization: Bearer YOUR_JWT_TOKEN' \
  --header 'content-type: application/json' \
  --data '{
  "restaurantName": "Saravana Bhavan",
  "deliveryArea": "T Nagar",
  "itemName": "Mini Tiffin",
  "qty": 5
}'
```

## 4. Checkout

```bash
curl --request POST \
  --url http://localhost:8082/api/cart/checkout \
  --header 'authorization: Bearer YOUR_JWT_TOKEN'
```

## 5. Customer order summary (normalized join)

```bash
curl --request GET \
  --url http://localhost:8082/api/cart/orders/summary \
  --header 'authorization: Bearer YOUR_JWT_TOKEN' \
  --header 'X-Auth-Id: YOUR_CUSTOMER_UUID'
```

Note: The gateway may forward `X-Auth-Id` from the JWT; if not, set it to the customer UUID from the token payload.
