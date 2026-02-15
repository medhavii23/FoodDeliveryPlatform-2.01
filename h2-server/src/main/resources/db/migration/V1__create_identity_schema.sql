-- Identity schema: single source of truth for users (customers, etc.)
CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.app_users (
    id UUID NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);
