# OdinSync Bounded Contexts

## Overview

OdinSync is designed as a **modular monolith** using **Domain-Driven Design (DDD)** and **Clean Architecture** principles. The application is divided into clearly defined **bounded contexts**, where each module owns a specific business capability, its domain model, application use cases, and persistence concerns.

Each bounded context encapsulates its business rules and exposes only well-defined interfaces to other modules. Modules collaborate through application services, ports, domain events, or integration events rather than directly accessing another module's internal implementation.

This architectural approach provides:

- Clear ownership of business capabilities
- Strong tenant isolation
- High cohesion within modules
- Low coupling between modules
- Independent evolution of business domains
- Easier testing and maintenance
- Future compatibility with microservice decomposition

---

# Platform Structure

The current high-level platform structure is shown below.

```text
OdinSync
├── Identity
├── Organization
├── CRM
├── Catalog
├── Inventory
├── Sales
├── Finance
├── Notification
└── Shared
```

Each module represents a bounded context with clearly defined responsibilities.

---

# Architectural Principles

## Business Capability Ownership

Every important business concept has exactly one owner.

Examples:

| Business Concept | Owner |
|------------------|--------|
| Authentication | Identity |
| User Accounts | Identity |
| Organization Profile | Organization |
| Customers | CRM |
| Products | Catalog |
| Inventory Stock | Inventory |
| Sales Orders | Sales |
| Invoices | Finance |
| Notifications | Notification |

Other modules may reference these concepts but must not modify or duplicate another module's business rules.

---

## Encapsulation

Every bounded context encapsulates:

- Domain entities
- Value objects
- Domain services
- Application services
- Repositories
- Database schema
- Business rules

Another module must never directly access:

- JPA entities
- Repositories
- Internal services
- Package-private classes
- Database tables

Communication should happen through explicit interfaces.

---

## Tenant Isolation

Every tenant owns an isolated business space.

All tenant-owned business data should contain a `tenantId`.

Typical repository queries:

```java
findByIdAndTenantId(UUID id, UUID tenantId);
```

Never query tenant-owned resources using only their identifier.

Incorrect:

```java
findById(customerId);
```

Correct:

```java
findByIdAndTenantId(customerId, tenantId);
```

This guarantees that one tenant cannot accidentally access another tenant's data.

---

## Independent Domain Models

Each bounded context owns its own domain model.

A module should not expose its internal entities to another module.

For example:

CRM owns:

- Customer
- Contact

Sales should not directly use the CRM `Customer` entity.

Instead, Sales may use a lightweight customer snapshot:

```text
CustomerSnapshot
---------------
customerId
customerName
billingAddress
```

CRM remains the source of truth.

---

## Explicit Dependencies

Dependencies between modules must always be intentional.

Allowed:

```text
Sales
    ↓
Catalog
```

Not allowed:

```text
Catalog
    ↓
Sales
```

Circular dependencies are prohibited.

---

## Future Microservice Compatibility

Although OdinSync is currently a modular monolith, every module should be designed so it can eventually become an independent microservice.

Each bounded context should therefore have:

- Clear ownership
- Public contracts
- Independent persistence
- Independent migrations
- Minimal shared state
- Event-driven communication where appropriate

---

# Identity Context

## Purpose

The Identity context manages application identities, authentication, authorization, and active login sessions.

It answers the question:

> **Who is the user, and what is the user allowed to do?**

Identity is the entry point into the platform and provides authentication and authorization services for every other module.

---

# Responsibilities

Identity owns:

- User accounts
- Credentials
- Password hashing
- Authentication
- Authorization
- Roles
- Role assignments
- Permissions
- JWT access tokens
- Refresh tokens
- Login sessions
- Session management
- Session revocation
- Replay detection
- Account status
- Security policies

Identity does not own Organization profile persistence. During tenant onboarding,
Identity orchestrates the registration workflow and calls the Organization
application contract to provision the initial Organization.

---

# Current Features

The current implementation includes:

- Organization registration
- Owner account creation
- BCrypt password hashing
- Spring Security AuthenticationManager
- OAuth2 Resource Server
- RSA (RS256) JWT signing
- JWT validation
- Role-Based Access Control (RBAC)
- Refresh token rotation
- Replay attack detection
- Logout
- Logout All Devices
- Session management
- Scheduled refresh token cleanup

---

# Domain Ownership

Identity owns the following aggregates:

```text
Tenant
│
└── User
      │
      ├── Role Assignment
      ├── Refresh Sessions
      └── Login History
```

Identity is the source of truth for these concepts.

---

# Does Not Own

Identity does **not** own:

- Organization profile
- Customers
- CRM contacts
- Products
- Inventory
- Orders
- Invoices
- Employee information

Those belong to their respective bounded contexts.

## Registration Collaboration

Tenant onboarding is a modular-monolith collaboration between Identity and
Organization.

```text
Identity Register Organization Use Case
    creates Tenant, Owner User, OWNER Role
    calls Organization Provisioning Use Case
        creates Organization aggregate
        saves through Organization repository adapter
```

Identity depends on the Organization application input port only. It must not
depend on Organization JPA entities, Spring Data repositories, persistence
mappers, or infrastructure adapters.

The Organization module is the source of truth for the `organizations` table and
is the only module that should map and persist the Organization aggregate.

---

# Public Contracts

Identity exposes services that other modules may consume.

Example:

```java
public interface CurrentActorProvider {

    UUID userId();

    UUID tenantId();

    Set<String> roles();

}
```

Example:

```java
public interface UserStatusQuery {

    boolean isActive(UUID tenantId, UUID userId);

}
```

Future interfaces may include:

```java
OrganizationMembershipQuery

PermissionQuery

CurrentTenantProvider

UserProfileQuery
```

---

# Dependencies

Identity should remain almost completely independent.

Other modules may depend on Identity for:

- Current authenticated user
- Current tenant
- Authorization
- User display information

Identity must **not** depend on:

- CRM
- Catalog
- Inventory
- Sales
- Finance
- Notification

This keeps authentication independent of business modules.

---

# Organization Context

## Purpose

The Organization context manages the tenant's business identity and global business configuration.

It answers the question:

> **Which business is operating inside this tenant?**

Identity manages **who** logs in.

Organization manages **which company** owns the data.

---

# Responsibilities

Organization owns:

- Business profile
- Organization name
- Display name
- Company logo
- Contact information
- Address
- Tax registration
- Business settings
- Time zone
- Currency
- Locale
- Organization status
- Tenant preferences

Future enhancements may include:

- Departments
- Branches
- Business units
- Fiscal configuration
- Regional settings

---

# Relationship Between Tenant and Organization

Although the initial implementation creates both simultaneously, they represent different concepts.

```text
Tenant
│
└── Organization
```

Tenant:

- Security boundary
- Data isolation
- Internal platform concept

Organization:

- Business identity
- Visible to end users
- Company profile

Current implementation:

```text
One Tenant
        │
        ▼
One Organization
```

The architecture should not assume this relationship can never evolve.

---

# Domain Ownership

Organization owns:

```text
Organization
│
├── Business Profile
├── Contact Details
├── Address
├── Currency
├── Timezone
└── Preferences
```

---

# Does Not Own

Organization does **not** own:

- Authentication
- Passwords
- JWT
- Roles
- Refresh Tokens
- Customers
- Products
- Inventory
- Orders
- Invoices

---

# Public Contracts

Example:

```java
public interface OrganizationQuery {

    OrganizationSummary findByTenantId(UUID tenantId);

}
```

Example:

```java
public record OrganizationSummary(

        UUID organizationId,

        UUID tenantId,

        String name,

        String defaultCurrency,

        String timeZone

) {}
```

Future services may include:

```text
OrganizationSettingsQuery

BusinessCalendarQuery

CurrencyProvider
```

---

# Dependencies

Organization depends only on Identity abstractions for:

- Current tenant
- Current authenticated user
- Authorization

Organization must remain independent of:

- CRM
- Catalog
- Inventory
- Sales
- Finance

---

# Identity and Organization Relationship

The relationship between the two contexts is illustrated below.

```text
                 Identity
          ┌───────────────────┐
          │ Authentication    │
          │ Users             │
          │ Roles             │
          │ Sessions          │
          └─────────┬─────────┘
                    │
         Provides authenticated user
                    │
                    ▼
             Organization
      ┌────────────────────────┐
      │ Business Profile       │
      │ Address                │
      │ Currency               │
      │ Timezone               │
      │ Business Settings      │
      └────────────────────────┘
```

Identity manages platform access.

Organization manages business identity.

Both contexts are independent while collaborating through explicit contracts.

---

# Summary

The **Identity** and **Organization** bounded contexts provide the foundation for the entire OdinSync platform.

- **Identity** secures the platform by managing users, authentication, authorization, and active sessions.
- **Organization** represents the business operating within a tenant and manages organization-wide configuration.

Together, these contexts establish tenant isolation, authentication, and business identity, allowing higher-level business modules such as CRM, Catalog, Inventory, Sales, and Finance to build on a stable and well-defined architectural foundation.


# CRM Context

## Purpose

The Customer Relationship Management (CRM) context manages an organization's relationships with prospective and existing customers.

It answers the question:

> **Who are we doing business with, and how do we manage those relationships?**

CRM is responsible for maintaining customer information, contacts, leads, opportunities, activities, and customer interactions throughout the sales lifecycle.

CRM is the **system of record** for customer information.

---

# Responsibilities

CRM owns:

- Customers
- Customer Accounts
- Contacts
- Leads
- Opportunities
- Activities
- Customer Notes
- Customer Addresses
- Customer Communication Details
- Customer Status
- Customer Classification
- Customer Ownership

Future enhancements may include:

- Customer Segments
- Customer Tags
- Marketing Campaigns
- Customer Timeline
- Customer Documents

---

# Core Concepts

## Customer

A customer represents a business or an individual with whom the organization conducts business.

Examples:

- Google
- Microsoft
- Amazon
- ABC Retail Pvt Ltd

Typical attributes:

- Customer Code
- Legal Name
- Display Name
- Billing Address
- Shipping Address
- Tax Information
- Credit Limit
- Status

Customers never authenticate into OdinSync.

---

## Contact

A contact is a person associated with a customer.

Example:

Customer

```
Google
```

Contacts

```
Rahul Sharma
Procurement Manager
```

```
Priya Gupta
Finance Manager
```

A customer may have many contacts.

Typical attributes:

- Name
- Email
- Phone
- Designation
- Department

Contacts do not log into OdinSync.

---

## Lead

A lead represents a potential customer who has not yet become an active customer.

Example:

```
ABC Manufacturing

Interested in ERP implementation
```

Possible lifecycle:

```
NEW

↓

CONTACTED

↓

QUALIFIED

↓

CONVERTED
```

Once converted, a lead creates a Customer.

---

## Opportunity

An opportunity represents a potential sale.

Example:

```
Customer

↓

Interested Products

↓

Expected Revenue

↓

Probability

↓

Expected Closing Date
```

Opportunities help sales teams forecast revenue.

---

## Activity

Activities represent interactions with customers.

Examples:

- Phone Call
- Email
- Meeting
- Product Demo
- Follow-up

Activities provide a historical timeline.

---

# Domain Ownership

CRM owns:

```
CRM

├── Customer
├── Contact
├── Lead
├── Opportunity
├── Activity
└── Notes
```

CRM is the single source of truth for customer information.

---

# Does Not Own

CRM does not own:

- Authentication
- Users
- Products
- Inventory
- Orders
- Invoices
- Payments

---

# Public Contracts

CRM should expose lightweight interfaces.

Example:

```java
public interface CustomerQuery {

    Optional<CustomerSummary> findById(
            UUID tenantId,
            UUID customerId);

}
```

Example:

```java
public record CustomerSummary(

        UUID customerId,

        String customerCode,

        String customerName,

        CustomerStatus status

) {}
```

Future interfaces:

```
CustomerSearch

CustomerLookup

CustomerValidation

CustomerCreditQuery
```

---

# Dependencies

CRM depends on:

- Identity (current user)
- Organization (tenant settings)

CRM must remain independent of:

- Inventory
- Sales
- Finance

---

# Relationships

```
CRM

├── Customer
│      │
│      ├── Contacts
│      ├── Opportunities
│      ├── Activities
│      └── Notes
│
└── Leads
```

---

# Catalog Context

## Purpose

The Catalog context manages the organization's products and services.

It answers the question:

> **What do we sell?**

Catalog owns product master data but does not manage inventory or stock quantities.

---

# Responsibilities

Catalog owns:

- Products
- Product Categories
- SKU
- Product Attributes
- Product Pricing
- Product Images
- Product Status
- Product Variants
- Units of Measure

Future enhancements:

- Product Bundles
- Product Options
- Product Documents
- Product Specifications

---

# Core Concepts

## Product

A product represents an item or service offered by the organization.

Examples:

- Laptop
- Printer
- Consulting Service
- Annual Subscription

Typical attributes:

- SKU
- Name
- Description
- Unit Price
- Tax Category
- Unit of Measure
- Status

---

## Category

Categories organize products.

Example:

```
Electronics

├── Laptop
├── Monitor
└── Keyboard
```

---

## SKU

SKU (Stock Keeping Unit) uniquely identifies a product.

Example:

```
LAP-001

MON-001

KEY-001
```

---

## Pricing

Catalog owns the default selling price.

Inventory never owns pricing.

Sales may copy pricing into an order but Catalog remains the source of truth.

---

# Domain Ownership

```
Catalog

├── Product
├── Category
├── SKU
├── Pricing
└── Product Attributes
```

---

# Does Not Own

Catalog does not own:

- Stock Quantity
- Warehouse
- Reservations
- Customers
- Orders
- Invoices

---

# Public Contracts

Example:

```java
public interface ProductQuery {

    Optional<ProductSummary> findById(
            UUID tenantId,
            UUID productId);

}
```

Example:

```java
public record ProductSummary(

        UUID productId,

        String sku,

        String name,

        BigDecimal unitPrice,

        ProductStatus status

) {}
```

Future interfaces:

```
ProductSearch

ProductAvailability

PricingQuery

CategoryQuery
```

---

# Dependencies

Catalog depends on:

- Identity
- Organization

Catalog must remain independent of:

- Inventory
- Sales
- Finance

---

# Relationship with Inventory

Catalog owns:

```
Product
```

Inventory owns:

```
Stock
```

Example:

```
Product

Laptop
```

Inventory

```
Warehouse A

125 Units
```

Catalog knows **what** is sold.

Inventory knows **how much** is available.

---

# CRM and Catalog Relationship

```
CRM

Customers

        │

        ▼

Sales

        ▲

Catalog

Products
```

CRM manages who buys.

Catalog manages what is sold.

Sales brings both together.

---

# Summary

The **CRM** and **Catalog** bounded contexts establish the commercial foundation of OdinSync.

- **CRM** owns customer relationships, contacts, leads, opportunities, and activities.
- **Catalog** owns products, categories, pricing, and product master data.

Neither module manages authentication, inventory, orders, or financial records. By maintaining strict ownership boundaries and exposing only lightweight contracts, these contexts remain loosely coupled and ready for future evolution into independent services if required.



---

# Inventory Context

## Purpose

The Inventory context manages the physical movement, availability, and storage of products.

It answers the question:

> **Where is inventory stored, how much is available, and how does inventory change over time?**

Inventory is responsible for maintaining accurate stock levels across warehouses while ensuring inventory consistency throughout the business lifecycle.

Inventory owns **stock**, not products.

Catalog defines *what* a product is.

Inventory defines *how much* of that product exists.

---

## Responsibilities

Inventory owns:

- Warehouses
- Storage Locations
- Inventory Balances
- Available Quantity
- Reserved Quantity
- Stock Movements
- Stock Adjustments
- Goods Receipt
- Goods Issue
- Inventory Transfers
- Inventory Reservations
- Reorder Levels
- Inventory Audit History

Future enhancements may include:

- Batch Tracking
- Serial Numbers
- Expiration Dates
- Bin Locations
- Multi-Warehouse Transfers
- Cycle Counting
- Inventory Forecasting

---

## Core Concepts

### Warehouse

A warehouse represents a physical or logical location where inventory is stored.

Examples:

- Gurgaon Warehouse
- Bangalore Warehouse
- Main Distribution Center

Each warehouse may contain multiple storage locations.

---

### Inventory Balance

Represents the current quantity of a product within a warehouse.

Example

```
Warehouse

↓

Laptop

↓

Available = 120

Reserved = 20

On Hand = 140
```

---

### Reservation

A reservation temporarily allocates inventory for another business process.

Example

```
Sales Order Created

↓

Reserve 5 Laptops

↓

Available decreases

↓

Reserved increases
```

Reservation prevents overselling.

---

### Stock Movement

Every inventory change creates a movement.

Examples:

- Purchase Receipt
- Sales Shipment
- Stock Adjustment
- Warehouse Transfer
- Customer Return

Inventory should never change silently.

Every modification should produce an auditable movement record.

---

## Domain Ownership

```
Inventory

├── Warehouse
├── Storage Location
├── Inventory Balance
├── Reservation
├── Stock Movement
├── Transfer
└── Adjustment
```

Inventory is the single source of truth for stock availability.

---

## Does Not Own

Inventory does **not** own:

- Products
- Product Pricing
- Customers
- Orders
- Invoices
- Authentication

Product information belongs to Catalog.

Customer information belongs to CRM.

---

## Public Contracts

Example:

```java
public interface InventoryAvailabilityQuery {

    InventoryAvailability findAvailability(
            UUID tenantId,
            UUID productId,
            UUID warehouseId);

}
```

Example:

```java
public record InventoryAvailability(

        UUID productId,

        BigDecimal available,

        BigDecimal reserved,

        BigDecimal onHand

) {}
```

Reservation service:

```java
public interface InventoryReservationService {

    ReservationResult reserve(
            ReserveInventoryCommand command);

    void release(
            UUID tenantId,
            UUID reservationId);

}
```

---

## Dependencies

Inventory depends on:

- Identity
- Organization
- Catalog

Inventory may read Product information from Catalog.

Inventory must never update Product data.

---

## Relationship with Catalog

Catalog owns:

```
Product

Category

Pricing
```

Inventory owns:

```
Warehouse

Quantity

Reservations
```

Example

```
Catalog

Laptop

↓

Inventory

Warehouse A

Available = 125
```

This separation keeps inventory independent of commercial information.

---

# Sales Context

## Purpose

The Sales context manages the complete customer ordering process.

It answers the question:

> **What has the customer agreed to purchase?**

Sales coordinates customer orders using customer information from CRM, products from Catalog, and inventory availability from Inventory.

Sales owns the commercial transaction.

---

## Responsibilities

Sales owns:

- Quotations
- Sales Orders
- Order Lines
- Discounts
- Order Status
- Shipping Details
- Delivery Status
- Order Approval
- Sales Workflow
- Sales History

Future enhancements may include:

- Promotions
- Coupons
- Return Merchandise Authorization (RMA)
- Subscription Orders
- Installment Orders
- Shipment Tracking

---

## Core Concepts

### Quotation

A quotation represents a commercial proposal.

Example

```
Customer

↓

Requested Products

↓

Price

↓

Discount

↓

Validity Date
```

A quotation may later become a Sales Order.

---

### Sales Order

A Sales Order represents a confirmed customer purchase.

Example

```
Order Number

Customer

Products

Quantity

Price

Status
```

Sales Order is the primary aggregate of the Sales context.

---

### Order Line

Each Sales Order contains one or more Order Lines.

Each line contains:

- Product
- Quantity
- Unit Price
- Discount
- Tax
- Total

---

### Order Status

Typical lifecycle:

```
Draft

↓

Confirmed

↓

Reserved

↓

Packed

↓

Shipped

↓

Delivered

↓

Completed
```

Cancelled orders follow a separate workflow.

---

## Domain Ownership

```
Sales

├── Quotation
├── Sales Order
├── Order Line
├── Shipment
├── Delivery
└── Discount
```

---

## Does Not Own

Sales does **not** own:

- Customers
- Products
- Warehouses
- Inventory
- Invoices
- Payments

Sales references these concepts but does not own them.

---

## Snapshot Principle

Sales should preserve historical information.

Example:

Customer changes company name.

Old Sales Orders should continue displaying the original name.

Therefore Sales stores snapshots such as:

```
Customer Name

Billing Address

Product Name

Unit Price

Tax Rate
```

instead of dynamically loading current values.

---

## Public Contracts

Example:

```java
public interface SalesOrderQuery {

    Optional<SalesOrderSummary> findById(
            UUID tenantId,
            UUID orderId);

}
```

Example:

```java
public record SalesOrderSummary(

        UUID orderId,

        String orderNumber,

        BigDecimal totalAmount,

        OrderStatus status

) {}
```

Future contracts:

```
OrderSearch

QuotationQuery

OrderApprovalService

ShipmentQuery
```

---

## Dependencies

Sales depends on:

- Identity
- Organization
- CRM
- Catalog
- Inventory

Sales orchestrates business workflows.

It does **not** own data from those modules.

---

## Sales Workflow

```
Customer

↓

CRM Validation

↓

Select Products

↓

Catalog Validation

↓

Check Inventory

↓

Reserve Stock

↓

Create Sales Order

↓

Await Shipment

↓

Complete Order
```

Sales coordinates the process but each module performs its own responsibility.

---

## Relationship Between CRM, Catalog, Inventory and Sales

```
CRM

Customer

      │

      ▼

Sales Order

▲           ▲

Catalog     Inventory

Product     Stock
```

Sales brings together customer, product, and inventory information while maintaining clear ownership boundaries.

---

## Business Rules

Examples of Sales rules:

- Customer must be active.
- Products must be active.
- Inventory must be available.
- Orders cannot be modified after completion.
- Cancelled orders release inventory reservations.
- Completed orders become read-only.

These rules belong to the Sales context.

---

## Summary

The **Inventory** and **Sales** contexts manage operational business processes.

- **Inventory** owns warehouses, stock balances, reservations, and inventory movements.
- **Sales** owns quotations, customer orders, order lines, and commercial workflows.

Inventory knows **how much** stock exists.

Catalog knows **what** the product is.

CRM knows **who** the customer is.

Sales coordinates all three while respecting bounded-context ownership.

---

# Finance Context

## Purpose

The Finance context manages the financial lifecycle of customer transactions.

It answers the question:

> **How much money is owed, received, refunded, or outstanding?**

Finance owns all financial documents and accounting-related records generated from business operations.

Sales is responsible for selling.

Finance is responsible for billing and payment.

---

# Responsibilities

Finance owns:

- Invoices
- Invoice Lines
- Payments
- Credit Notes
- Debit Notes
- Receivables
- Payment Allocations
- Tax Calculations
- Financial Document Numbering
- Financial Audit History

Future enhancements:

- General Ledger
- Journal Entries
- Accounts Payable
- Vendor Bills
- Expense Management
- Bank Reconciliation
- Financial Reporting

---

# Core Concepts

## Invoice

An invoice represents a financial request for payment.

Typical fields:

- Invoice Number
- Customer
- Invoice Date
- Due Date
- Currency
- Invoice Lines
- Tax Amount
- Total Amount
- Outstanding Amount
- Status

Invoice status example:

```text
Draft

↓

Issued

↓

Partially Paid

↓

Paid

↓

Cancelled
```

---

## Invoice Line

Each invoice consists of one or more invoice lines.

Each line contains:

- Product
- Description
- Quantity
- Unit Price
- Discount
- Tax
- Total

Finance stores immutable financial snapshots.

Future product changes must never modify historical invoices.

---

## Payment

A payment records money received from a customer.

Examples:

- Bank Transfer
- Cash
- UPI
- Credit Card
- Payment Gateway

A payment may be allocated across multiple invoices.

---

## Credit Note

A credit note reduces an existing invoice.

Typical reasons:

- Product Return
- Pricing Correction
- Service Cancellation

---

## Debit Note

A debit note increases an amount owed.

Typical reasons:

- Additional Charges
- Underbilling
- Late Fees

---

# Domain Ownership

```
Finance

├── Invoice
├── Invoice Line
├── Payment
├── Credit Note
├── Debit Note
└── Receivable
```

---

# Does Not Own

Finance does **not** own:

- Products
- Customers
- Warehouses
- Orders
- Authentication

Finance references these concepts through contracts.

---

# Public Contracts

Example:

```java
public interface InvoiceQuery {

    Optional<InvoiceSummary> findById(
            UUID tenantId,
            UUID invoiceId);

}
```

Example:

```java
public record InvoiceSummary(

        UUID invoiceId,

        String invoiceNumber,

        BigDecimal totalAmount,

        BigDecimal outstandingAmount,

        InvoiceStatus status

) {}
```

Future contracts:

```
PaymentQuery

InvoiceSearch

ReceivableQuery

FinancialSummary
```

---

# Dependencies

Finance depends on:

- Identity
- Organization
- CRM
- Sales

Finance should not directly depend on:

- Inventory
- Catalog

Sales provides commercial information.

Finance creates financial records.

---

# Relationship with Sales

Sales owns:

```
Sales Order
```

Finance owns:

```
Invoice
```

Workflow

```
Sales Order

↓

Delivered

↓

Invoice Created

↓

Customer Pays

↓

Payment Recorded
```

Sales never creates invoices directly.

Finance owns invoicing.

---

# Notification Context

## Purpose

The Notification context manages communication between OdinSync and external users.

It answers the question:

> **How should the platform communicate business events?**

Notification owns message delivery.

It does **not** decide when notifications should be sent.

---

# Responsibilities

Notification owns:

- Email
- SMS
- Push Notifications
- Notification Templates
- Delivery Status
- Retry Policies
- Provider Integration
- Communication History

Future enhancements:

- WhatsApp
- Slack
- Microsoft Teams
- Webhooks
- In-App Notifications

---

# Core Concepts

## Notification

A notification represents a request to send a message.

Examples:

- Welcome Email
- Password Reset
- Invoice Email
- Order Confirmation
- Shipment Notification

---

## Template

Templates define reusable messages.

Example:

```
Welcome Email

Hello {{customerName}}

Welcome to OdinSync.
```

---

## Delivery

Delivery tracks:

- Sent
- Delivered
- Failed
- Retried

---

# Domain Ownership

```
Notification

├── Email
├── SMS
├── Push
├── Templates
└── Delivery History
```

---

# Does Not Own

Notification does **not** own:

- Authentication
- Orders
- Customers
- Products
- Invoices

It only delivers messages.

---

# Public Contracts

Example:

```java
public interface NotificationGateway {

    void send(NotificationRequest request);

}
```

Example:

```java
public record NotificationRequest(

        UUID tenantId,

        NotificationChannel channel,

        String template,

        String recipient,

        Map<String, Object> variables

) {}
```

---

# Dependencies

Notification depends on:

- Identity
- Organization

Every other module may use Notification.

Notification should never depend on business logic.

---

# Example Flow

```
Invoice Created

↓

Finance

↓

Notification

↓

Email Provider

↓

Customer
```

Finance decides when to notify.

Notification decides how to deliver.

---

# Shared Context

## Purpose

Shared contains reusable technical components used across multiple bounded contexts.

Shared is **not** a business module.

It exists to avoid duplication of cross-cutting technical concerns.

---

# May Contain

Examples:

- BaseEntity
- AuditableEntity
- Clock
- UUID Utilities
- Pagination
- Common Exceptions
- Validation Helpers
- Correlation IDs
- Shared DTO Infrastructure
- API Response Models
- Error Handling

---

# Must Never Contain

Shared must **not** contain:

- Customer business logic
- Product business logic
- Sales rules
- Inventory rules
- Finance calculations

Business logic belongs inside its owning bounded context.

---

# Rule of Thumb

Ask:

> Is this concept owned by one business module?

If yes:

It does **not** belong in Shared.

Ask:

> Is this purely technical and reusable everywhere?

If yes:

Shared is appropriate.

---

# Shared Package Structure

Example

```
shared

├── exception
├── validation
├── security
├── time
├── pagination
├── auditing
├── configuration
└── util
```

---

# Dependencies

Every module may depend on Shared.

Shared must never depend on any business module.

```
Shared

▲

Identity
Organization
CRM
Catalog
Inventory
Sales
Finance
Notification
```

Shared remains at the bottom of the dependency graph.

---

# Summary

The final bounded contexts complete OdinSync's business architecture.

**Finance** owns:

- Invoices
- Payments
- Credit Notes
- Receivables

**Notification** owns:

- Email
- SMS
- Push Notifications
- Templates
- Delivery

**Shared** owns:

- Cross-cutting technical utilities only

Each module has a single, clearly defined responsibility and exposes only stable contracts to the rest of the platform. This separation minimizes coupling, improves maintainability, and preserves a clear migration path from a modular monolith to independently deployable microservices.


---

# Module Dependency Matrix

The following matrix illustrates which bounded contexts may depend on others.

| Context | Identity | Organization | CRM | Catalog | Inventory | Sales | Finance | Notification | Shared |
|----------|:--------:|:------------:|:---:|:-------:|:---------:|:-----:|:-------:|:------------:|:------:|
| Identity | — | | | | | | | | ✓ |
| Organization | ✓ | — | | | | | | | ✓ |
| CRM | ✓ | ✓ | — | | | | | ✓ | ✓ |
| Catalog | ✓ | ✓ | | — | | | | | ✓ |
| Inventory | ✓ | ✓ | | ✓ | — | | | | ✓ |
| Sales | ✓ | ✓ | ✓ | ✓ | ✓ | — | | ✓ | ✓ |
| Finance | ✓ | ✓ | ✓ | | | ✓ | — | ✓ | ✓ |
| Notification | ✓ | ✓ | | | | | | — | ✓ |
| Shared | | | | | | | | | — |

The dependency direction always flows toward lower-level foundational services.

Business modules should never create circular dependencies.

---

# Dependency Graph

A simplified dependency graph is shown below.

```text
                         Shared
                            ▲
                            │
                     Identity
                            ▲
                            │
                    Organization
                   ▲            ▲
                  │              │
               CRM           Catalog
                  │              │
                  └──────┬───────┘
                         │
                    Inventory
                         ▲
                         │
                       Sales
                         ▲
                         │
                      Finance

Notification is used by all business modules.
```

---

# Allowed Dependencies

Examples of acceptable interactions:

Sales → CRM

```text
Validate customer
```

Sales → Catalog

```text
Retrieve product information
```

Sales → Inventory

```text
Reserve stock
```

Finance → Sales

```text
Read completed sales order
```

Finance → CRM

```text
Read customer information
```

CRM → Notification

```text
Send welcome email
```

---

# Forbidden Dependencies

The following are architectural violations.

Inventory directly modifying Sales Orders.

```text
Inventory

↓

Sales Database
```

Sales updating Inventory tables.

```text
UPDATE inventory_stock
```

Finance modifying Customer records.

```text
Customer.name = ...
```

Catalog modifying warehouse quantities.

```text
inventory.quantity = 500
```

CRM reading Product repositories.

These responsibilities belong to other bounded contexts.

---

# Cross-Context Communication

Bounded contexts communicate through explicit contracts.

Preferred mechanisms:

## Query Interfaces

Used when another module needs read-only information.

Example:

```java
CustomerSummary customer =
        customerQuery.findById(tenantId, customerId);
```

---

## Command Interfaces

Used when another module requests a business action.

Example:

```java
inventoryReservationService.reserve(command);
```

The receiving module performs its own business validation.

---

## Domain Events

Domain events communicate important business changes.

Examples:

```text
CustomerCreated

SalesOrderConfirmed

InventoryReserved

InvoiceIssued

PaymentReceived
```

Events represent something that has already happened.

---

## Integration Events

When OdinSync eventually becomes distributed, integration events will replace in-process communication where appropriate.

Examples:

```text
SalesOrderConfirmedIntegrationEvent

InvoiceIssuedIntegrationEvent

PaymentReceivedIntegrationEvent
```

---

# Transaction Boundaries

Every bounded context owns its own transactions.

Example:

Sales transaction

```text
Create Sales Order

+

Create Order Lines
```

Inventory transaction

```text
Reserve Inventory
```

Finance transaction

```text
Generate Invoice
```

Business transactions should not directly update multiple bounded contexts within a single database transaction.

Long-running workflows should use orchestration or events.

---

# Data Ownership

Each bounded context owns its own data.

Examples:

Identity

```
users
roles
sessions
refresh_tokens
```

CRM

```
customers
contacts
leads
activities
```

Catalog

```
products
categories
pricing
```

Inventory

```
warehouses
inventory_balance
stock_movements
reservations
```

Sales

```
sales_orders
order_lines
quotations
```

Finance

```
invoices
payments
credit_notes
```

No module should modify another module's tables directly.

---

# Snapshot Principle

Business documents should preserve historical information.

Example:

Product

```
Laptop
₹75,000
```

One year later:

```
Laptop Pro
₹82,000
```

Existing Sales Orders must continue showing:

```
Laptop

₹75,000
```

Sales therefore stores:

- Product Name
- Unit Price
- Tax Rate

instead of loading current Product data.

Finance follows the same principle for invoices.

---

# Event Ownership

Every event belongs to exactly one bounded context.

Examples:

Identity

```
UserRegistered

PasswordChanged

UserDisabled
```

CRM

```
CustomerCreated

LeadConverted
```

Catalog

```
ProductCreated

PriceUpdated
```

Inventory

```
InventoryReserved

StockAdjusted
```

Sales

```
SalesOrderConfirmed

SalesOrderCancelled
```

Finance

```
InvoiceIssued

PaymentReceived
```

Notification

```
NotificationSent

NotificationFailed
```

The publishing module owns the event definition.

---

# Package Organization

Each module follows Clean Architecture.

Example:

```text
crm

├── domain
├── application
├── infrastructure
├── presentation
└── configuration
```

Public contracts should be exposed through dedicated packages.

Example:

```text
crm.application.api
```

Other modules must depend only on these contracts.

---

# Database Organization

Initially OdinSync uses a single PostgreSQL database.

Logical ownership should still remain separated.

Recommended naming:

```text
identity_users

identity_roles

crm_customers

catalog_products

inventory_stock

sales_orders

finance_invoices
```

As the platform grows, modules may migrate into separate schemas.

Eventually they may become separate databases if extracted into microservices.

---

# Microservice Readiness

Every bounded context is designed to become an independent service.

Possible future services:

```text
identity-service

organization-service

crm-service

catalog-service

inventory-service

sales-service

finance-service

notification-service
```

Migration becomes significantly easier because:

- Ownership is already defined.
- Dependencies are explicit.
- Public contracts already exist.
- Business logic is isolated.
- Database ownership is respected.

No major redesign should be required.

---

# Architecture Review Checklist

Before introducing new functionality, verify:

- Does this belong to an existing bounded context?
- Who owns this business concept?
- Are we modifying another module's data?
- Are we introducing circular dependencies?
- Can this module evolve independently?
- Are we exposing only public contracts?
- Does tenant isolation remain intact?

If any answer is unclear, revisit the bounded-context definitions before implementation.

---

# Summary

OdinSync is organized around bounded contexts, where each module owns a clearly defined business capability.

| Context | Primary Responsibility |
|----------|------------------------|
| Identity | Users, Authentication, Authorization, Sessions |
| Organization | Business Profile, Tenant Settings |
| CRM | Customers, Contacts, Leads, Opportunities |
| Catalog | Products, Categories, Pricing |
| Inventory | Warehouses, Stock, Reservations |
| Sales | Quotations, Orders, Fulfillment |
| Finance | Invoices, Payments, Receivables |
| Notification | Email, SMS, Push Notifications |
| Shared | Cross-cutting Technical Components |

By enforcing these boundaries:

- Every business concept has a single owner.
- Modules remain loosely coupled.
- Cross-module communication is explicit.
- Tenant isolation is preserved.
- Historical business data remains immutable where required.
- The modular monolith remains maintainable as the codebase grows.
- Future migration to microservices becomes an architectural evolution rather than a complete redesign.

This document serves as the authoritative reference for defining ownership, responsibilities, and collaboration between bounded contexts within the OdinSync platform.ffds   
