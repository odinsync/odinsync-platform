# Event Storming

This document captures the key business events, commands, actors, policies, and aggregates in OdinSync.

Event storming helps us understand business workflows before designing APIs, databases, or services.

---

## 1. Core Business Workflow

Business Owner registers organization  
→ Tenant is created  
→ Owner user is created  
→ Customer is created  
→ Product is added  
→ Stock is added  
→ Sales order is created  
→ Inventory is reserved  
→ Invoice is generated  
→ Payment is recorded  
→ Business data becomes available for reporting

---

## 2. Actors

- Business Owner
- Tenant Admin
- Sales Manager
- Inventory Manager
- Accountant
- Employee
- System
- Future AI Copilot

---

## 3. Commands

Commands represent user or system actions.

### Identity & Access

- Register Organization
- Login User
- Invite User
- Assign Role
- Disable User

### Organization

- Create Organization Profile
- Update Organization Profile

### CRM

- Create Customer
- Update Customer
- Search Customer

### Catalog

- Create Product
- Update Product
- Create Category
- Update Product Price

### Inventory

- Add Stock
- Reserve Stock
- Release Reserved Stock
- Adjust Stock

### Sales

- Create Sales Order
- Confirm Sales Order
- Cancel Sales Order

### Finance

- Generate Invoice
- Record Payment
- Mark Invoice As Paid
- Mark Invoice As Partially Paid

---

## 4. Domain Events

Domain events represent facts that already happened.

### Identity & Access

- OrganizationRegistered
- TenantCreated
- UserCreated
- UserLoggedIn
- UserInvited
- RoleAssigned
- UserDisabled

### CRM

- CustomerCreated
- CustomerUpdated

### Catalog

- ProductCreated
- ProductUpdated
- CategoryCreated
- ProductPriceChanged

### Inventory

- StockAdded
- StockReserved
- StockReservationFailed
- ReservedStockReleased
- StockAdjusted

### Sales

- SalesOrderCreated
- SalesOrderConfirmed
- SalesOrderCancelled

### Finance

- InvoiceGenerated
- PaymentRecorded
- InvoicePartiallyPaid
- InvoicePaid

---

## 5. Policies

Policies describe automatic reactions to events.

### Registration Policy

When `OrganizationRegistered` happens:

- Create tenant
- Create organization
- Create owner user
- Assign owner role

### Sales Order Policy

When `SalesOrderCreated` happens:

- Validate customer
- Validate products
- Calculate order total

### Inventory Reservation Policy

When `SalesOrderConfirmed` happens:

- Check available stock
- Reserve inventory
- If stock is unavailable, reject confirmation

### Invoice Policy

When `StockReserved` happens:

- Generate invoice for the sales order

### Payment Policy

When `PaymentRecorded` happens:

- Update invoice paid amount
- Mark invoice as partially paid or paid

### Cancellation Policy

When `SalesOrderCancelled` happens:

- Release reserved stock
- Cancel invoice if applicable

---

## 6. Aggregates

Aggregates protect business rules.

### Tenant

Protects:
- Tenant identity
- Tenant status
- Data ownership boundary

### User

Protects:
- Authentication identity
- User status
- Role assignment

### Organization

Protects:
- Business profile
- Tenant organization details

### Customer

Protects:
- Customer identity
- Contact details

### Product

Protects:
- Product identity
- SKU uniqueness within tenant
- Product status

### InventoryItem

Protects:
- Available quantity
- Reserved quantity
- Stock reservation rules

### SalesOrder

Protects:
- Order lifecycle
- Order status
- Order total
- Order items

### Invoice

Protects:
- Invoice amount
- Paid amount
- Payment status

### Payment

Protects:
- Payment amount
- Payment method
- Payment date

---

## 7. MVP Event Flow

```text
RegisterOrganization
        ↓
OrganizationRegistered
        ↓
TenantCreated
        ↓
UserCreated
        ↓
RoleAssigned

CreateCustomer
        ↓
CustomerCreated

CreateProduct
        ↓
ProductCreated

AddStock
        ↓
StockAdded

CreateSalesOrder
        ↓
SalesOrderCreated

ConfirmSalesOrder
        ↓
StockReserved
        ↓
SalesOrderConfirmed
        ↓
InvoiceGenerated

RecordPayment
        ↓
PaymentRecorded
        ↓
InvoicePaid / InvoicePartiallyPaid