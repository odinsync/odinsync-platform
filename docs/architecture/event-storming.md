OdinSync Event Storming

Overview

This document identifies the core business events, commands, actors, policies, and aggregates that drive the OdinSync platform.

It serves as the bridge between business requirements and implementation.

The goals of event storming are to:

- Discover business workflows
- Identify aggregates
- Define bounded-context interactions
- Minimize coupling between modules
- Prepare for future event-driven architecture
- Simplify eventual microservice extraction

This document describes business behavior rather than technical implementation.

⸻

Event Storming Concepts

Actor

An actor initiates a business action.

Examples:

- Organization Owner
- Administrator
- Sales Representative
- Inventory Manager
- Accountant
- Customer (external)
- System Scheduler

⸻

Command

A command represents an intention to perform work.

Commands are written in the imperative.

Examples:

- Register Organization
- Login User
- Create Customer
- Create Product
- Reserve Inventory
- Create Sales Order
- Issue Invoice

Commands may succeed or fail.

⸻

Aggregate

An aggregate protects business consistency.

Examples:

- User
- Organization
- Customer
- Product
- Warehouse
- Sales Order
- Invoice

Every command targets exactly one aggregate.

⸻

Domain Event

A domain event records something that has already happened.

Events are written in the past tense.

Examples:

- UserRegistered
- CustomerCreated
- ProductActivated
- InventoryReserved
- SalesOrderConfirmed
- InvoiceIssued
- PaymentReceived

Events are immutable.

⸻

Policy

A policy reacts to one event by issuing another command.

Example:

SalesOrderConfirmed
        │
        ▼
Reserve Inventory

Policies automate business workflows.

⸻

Platform Event Flow

Actor
   │
   ▼
Command
   │
   ▼
Aggregate
   │
   ▼
Domain Event
   │
   ▼
Policy
   │
   ▼
Next Command

⸻

Identity Events

Workflow

Organization Owner
        │
Register Organization
        │
Tenant Aggregate
        │
TenantCreated
        │
Organization Aggregate
        │
OrganizationCreated
        │
User Aggregate
        │
OwnerUserCreated
        │
Role Assignment
        │
OwnerRoleAssigned
        │
Authentication Ready

⸻

Commands

- Register Organization
- Authenticate User
- Refresh Access Token
- Logout Session
- Logout All Sessions
- Disable User
- Change Password

⸻

Events

- TenantCreated
- OrganizationCreated
- UserRegistered
- UserAuthenticated
- AccessTokenIssued
- RefreshTokenRotated
- SessionCreated
- SessionRevoked
- PasswordChanged
- UserDisabled

⸻

Organization Events

Commands

- Update Organization
- Change Currency
- Change Time Zone
- Update Business Settings

⸻

Events

- OrganizationUpdated
- BusinessSettingsUpdated
- CurrencyChanged
- TimeZoneChanged

⸻

CRM Events

Customer Creation

Sales Representative
        │
Create Customer
        │
Customer Aggregate
        │
CustomerCreated

⸻

Lead Conversion

Lead
        │
Convert Lead
        │
Customer Aggregate
        │
CustomerCreated
        │
LeadConverted

⸻

Commands

- Create Customer
- Update Customer
- Archive Customer
- Create Contact
- Create Lead
- Convert Lead
- Create Opportunity
- Complete Activity

⸻

Events

- CustomerCreated
- CustomerUpdated
- CustomerArchived
- ContactCreated
- LeadCreated
- LeadConverted
- OpportunityCreated
- OpportunityWon
- OpportunityLost
- ActivityCompleted

⸻

Catalog Events

Product Creation

Product Manager
        │
Create Product
        │
Product Aggregate
        │
ProductCreated

⸻

Commands

- Create Product
- Update Product
- Activate Product
- Deactivate Product
- Update Price

⸻

Events

- ProductCreated
- ProductUpdated
- ProductActivated
- ProductDeactivated
- ProductPriceChanged

⸻

Inventory Events

Inventory Reservation

Sales Order Confirmed
        │
Reserve Inventory
        │
Inventory Aggregate
        │
InventoryReserved

⸻

Goods Receipt

Receive Stock
        │
Inventory Aggregate
        │
InventoryReceived

⸻

Commands

- Receive Inventory
- Adjust Inventory
- Reserve Inventory
- Release Reservation
- Transfer Inventory

⸻

Events

- InventoryReceived
- InventoryAdjusted
- InventoryReserved
- ReservationReleased
- InventoryTransferred

⸻

Sales Events

Sales Order Workflow

Sales Representative
        │
Create Sales Order
        │
Sales Order Aggregate
        │
SalesOrderCreated
        │
Confirm Sales Order
        │
SalesOrderConfirmed
        │
Reserve Inventory
        │
InventoryReserved
        │
Ready for Fulfillment

⸻

Commands

- Create Quotation
- Approve Quotation
- Create Sales Order
- Confirm Sales Order
- Cancel Sales Order
- Ship Order
- Complete Order

⸻

Events

- QuotationCreated
- QuotationApproved
- SalesOrderCreated
- SalesOrderConfirmed
- SalesOrderCancelled
- OrderShipped
- OrderDelivered
- OrderCompleted

⸻

Finance Events

Invoice Workflow

SalesOrderCompleted
        │
Issue Invoice
        │
Invoice Aggregate
        │
InvoiceIssued
        │
Receive Payment
        │
PaymentReceived

⸻

Commands

- Issue Invoice
- Cancel Invoice
- Record Payment
- Issue Credit Note
- Issue Debit Note

⸻

Events

- InvoiceIssued
- InvoiceCancelled
- PaymentReceived
- CreditNoteIssued
- DebitNoteIssued

⸻

Notification Events

Notification normally reacts to events from other bounded contexts.

Examples:

UserRegistered
↓
Send Welcome Email
InvoiceIssued
↓
Send Invoice Email
SalesOrderConfirmed
↓
Send Order Confirmation

⸻

Commands

- Send Email
- Send SMS
- Send Push Notification

⸻

Events

- NotificationSent
- NotificationFailed

⸻

Cross-Context Policies

Customer Created

CustomerCreated
        │
Create Default Preferences

⸻

Sales Order Confirmed

SalesOrderConfirmed
        │
Reserve Inventory

⸻

Inventory Reserved

InventoryReserved
        │
Continue Fulfillment

⸻

Order Delivered

OrderDelivered
        │
Issue Invoice

⸻

Invoice Issued

InvoiceIssued
        │
Send Invoice Email

⸻

Payment Received

PaymentReceived
        │
Update Outstanding Balance

⸻

Event Ownership

Each event belongs to exactly one bounded context.

Event	Owner
UserRegistered	Identity
CustomerCreated	CRM
ProductCreated	Catalog
InventoryReserved	Inventory
SalesOrderConfirmed	Sales
InvoiceIssued	Finance
NotificationSent	Notification

Only the owning bounded context may define the event’s meaning.

⸻

Event Naming Guidelines

Use past tense.

Good examples:

- CustomerCreated
- ProductActivated
- InventoryReserved
- SalesOrderConfirmed
- InvoiceIssued

Avoid:

- CreateCustomer
- ReserveInventory
- ConfirmOrder

Those are commands, not events.

⸻

Future Event Bus

The current modular monolith may publish events in-process.

Future architecture may introduce:

Sales
↓
Kafka Topic
↓
Finance
↓
Notification

Possible technologies:

- Spring Application Events
- Spring Modulith Events
- Kafka
- RabbitMQ
- Google Pub/Sub

The business events defined in this document remain unchanged regardless of transport.

⸻

Summary

Event storming identifies how business capabilities collaborate through commands and events.

Each bounded context owns its aggregates and publishes its own domain events.

Policies react to those events to coordinate business workflows while preserving loose coupling.

This approach prepares OdinSync for:

- Modular monolith development
- Event-driven workflows
- Future microservice extraction
- Saga-based orchestration
- Reliable business process automation

