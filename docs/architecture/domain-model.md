# OdinSync Domain Model

## Core Aggregate Roots

### Tenant

Represents one company/account using OdinSync.

Owns:
- Organization
- Users
- Roles
- Business data

Rules:
- Every tenant has isolated data.
- Every business entity must belong to one tenant.

---

### Organization

Represents the legal/business profile of a tenant.

Fields:
- id
- tenantId
- name
- legalName
- gstNumber
- address
- phone
- email

---

### User

Represents a person who can access OdinSync.

Fields:
- id
- tenantId
- email
- passwordHash
- fullName
- status

Rules:
- User email must be unique.
- User belongs to one tenant.
- User can have multiple roles.

---

### Customer

Represents a customer of the tenant.

Fields:
- id
- tenantId
- name
- email
- phone
- billingAddress
- shippingAddress

Rules:
- Customer belongs to one tenant.
- Customer can place multiple sales orders.

---

### Product

Represents a sellable item.

Fields:
- id
- tenantId
- sku
- name
- description
- categoryId
- sellingPrice
- status

Rules:
- SKU must be unique within a tenant.
- Product belongs to Catalog, not Inventory.

---

### InventoryItem

Represents stock for a product.

Fields:
- id
- tenantId
- productId
- warehouseId
- availableQuantity
- reservedQuantity

Rules:
- Available quantity cannot be negative.
- Reserved quantity cannot be negative.
- Stock reservation happens before confirming a sales order.

---

### SalesOrder

Represents a customer order.

Fields:
- id
- tenantId
- customerId
- orderNumber
- status
- totalAmount

Rules:
- Sales order must have at least one item.
- Sales order cannot be confirmed if inventory is unavailable.
- Confirmed order reserves inventory.

---

### Invoice

Represents money owed by customer.

Fields:
- id
- tenantId
- salesOrderId
- invoiceNumber
- status
- totalAmount
- paidAmount

Rules:
- Invoice is generated from a confirmed sales order.
- Invoice can be unpaid, partially paid, or paid.

---

### Payment

Represents money received from customer.

Fields:
- id
- tenantId
- invoiceId
- amount
- paymentMode
- paymentDate

Rules:
- Payment amount must be positive.
- Payment updates invoice paid amount.