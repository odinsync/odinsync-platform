# API Overview

OdinSync APIs will follow REST conventions and will be versioned from day one.

Base path:

/api/v1

## Authentication

POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh-token

## Organization

GET /api/v1/organization
PUT /api/v1/organization

## Customers

POST /api/v1/customers
GET /api/v1/customers
GET /api/v1/customers/{id}
PUT /api/v1/customers/{id}
DELETE /api/v1/customers/{id}

## Catalog

POST /api/v1/categories
GET /api/v1/categories

POST /api/v1/products
GET /api/v1/products
GET /api/v1/products/{id}
PUT /api/v1/products/{id}
DELETE /api/v1/products/{id}

## Inventory

POST /api/v1/inventory/stock
GET /api/v1/inventory/items
POST /api/v1/inventory/reservations
POST /api/v1/inventory/reservations/{id}/release

## Sales

POST /api/v1/sales-orders
GET /api/v1/sales-orders
GET /api/v1/sales-orders/{id}
POST /api/v1/sales-orders/{id}/confirm
POST /api/v1/sales-orders/{id}/cancel

## Finance

POST /api/v1/invoices
GET /api/v1/invoices
GET /api/v1/invoices/{id}

POST /api/v1/payments
GET /api/v1/payments
GET /api/v1/payments/{id}

## API Rules

- All protected APIs require JWT authentication.
- Tenant context is extracted from JWT.
- Clients must not send tenant_id.
- All list APIs must support pagination.
- APIs should return consistent error responses.
- APIs should use request/response DTOs.
- Public APIs must be versioned.