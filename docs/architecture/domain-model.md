OdinSync Domain Model – Core Business Terms

Overview

OdinSync is a multi-tenant SaaS platform. Understanding the core business entities is essential before implementing business modules such as CRM, Catalog, Inventory, Sales, Finance, and HR.

This document explains the purpose of the primary domain concepts used throughout the platform and how they relate to each other.

⸻

Tenant

Definition

A Tenant is the highest level of isolation within OdinSync.

Every customer that registers on OdinSync receives a dedicated tenant. All business data belongs to exactly one tenant.

The tenant exists to enforce data isolation, ensuring that one organization can never access another organization’s data.

Examples of tenant-scoped data include:

* Users
* Customers
* Products
* Inventory
* Orders
* Invoices
* Reports
* Settings

Every business entity should contain a tenantId.

Example:

Tenant A
│
├── Customers
├── Products
├── Orders
└── Users
Tenant B
│
├── Customers
├── Products
├── Orders
└── Users

The tenant is primarily a backend concept and is usually invisible to end users.

⸻

Organization

Definition

An Organization represents the business that owns the tenant.

Unlike the tenant, which exists for security and isolation, the organization represents the company’s business identity.

Typical information includes:

* Company name
* Logo
* Address
* Email
* Phone number
* GST/VAT number
* Time zone
* Currency
* Business settings

Example:

Organization
ABC Pvt Ltd
Address
GST Number
Currency
Timezone

The organization is visible throughout the application’s user interface.

⸻

User

Definition

A User is a person who can authenticate into OdinSync.

A user has:

* Login credentials
* Assigned roles
* Permissions
* Active sessions
* Authentication history

Examples include:

* Owner
* Administrator
* Sales executive
* Inventory manager
* Accountant
* HR manager

A user is not a customer or a CRM contact.

Typical user information:

* Email
* Password hash
* Status
* Roles
* Tenant
* Authentication settings

⸻

Role

Definition

A Role defines what a user is allowed to do within OdinSync.

Examples:

* OWNER
* ADMIN
* MEMBER

Roles determine access to protected APIs and application features.

Example:

Role	Example Permissions
OWNER	Full tenant administration
ADMIN	User and business management
MEMBER	Business operations based on assigned permissions

Roles are assigned to users and evaluated during authorization.

⸻

Permission

Definition

A Permission represents a specific capability within the application.

Examples:

* customer.read
* customer.create
* product.update
* invoice.delete

Permissions provide fine-grained authorization.

While OdinSync currently uses role-based authorization, permissions can be introduced later without redesigning the identity model.

⸻

Session

Definition

A Session represents one authenticated login on a specific device.

Each session maintains:

* Refresh token
* Device information
* User agent
* IP address
* Last activity
* Expiration
* Status

Examples:

John
├── MacBook
├── iPhone
└── Office Laptop

Each device has its own independent session.

Logging out from one device does not affect the others unless “Logout All Devices” is used.

⸻

Customer

Definition

A Customer is a business or individual that purchases products or services from the organization.

Customers belong to the CRM domain.

Examples:

* Google
* Microsoft
* ABC Retail
* Jane Doe

Customers do not authenticate into OdinSync.

Typical customer information:

* Company name
* Customer code
* Billing address
* Shipping address
* Credit limit
* GST/VAT
* Status

⸻

Contact

Definition

A Contact is a person associated with a customer.

Example:

Customer

Google

Contacts

Rahul Sharma
Procurement Manager
Priya Gupta
Finance Lead

Contacts store:

* Name
* Email
* Phone
* Designation

Contacts do not log into OdinSync.

⸻

Employee

Definition

An Employee represents a worker employed by the organization.

Employees belong to the HR domain.

Typical information:

* Employee ID
* Department
* Designation
* Salary
* Joining date
* Manager
* Leave balance

An employee may also have a user account, but the concepts are independent.

Examples:

Employee
↓
John Smith
Department
Sales
Salary
₹12,00,000

⸻

Product

Definition

A Product is an item or service sold by the organization.

Products belong to the Catalog domain.

Typical information:

* SKU
* Name
* Description
* Price
* Tax category
* Status

Products are later used by Inventory and Sales modules.

⸻

Inventory Item

Definition

An Inventory Item represents the available stock of a product.

Typical information:

* Product
* Warehouse
* Quantity
* Reserved quantity
* Available quantity
* Reorder level

Inventory belongs to the Inventory domain.

⸻

Sales Order

Definition

A Sales Order represents a customer’s request to purchase products.

Typical information:

* Customer
* Order number
* Order lines
* Quantity
* Price
* Taxes
* Discounts
* Status

Sales orders belong to the Sales domain.

⸻

Invoice

Definition

An Invoice represents the financial document generated for a sale.

Invoices belong to the Finance domain.

Typical information:

* Invoice number
* Customer
* Amount
* Taxes
* Payment status
* Due date

⸻

Relationship Between Core Business Entities

Tenant
│
├── Organization
│
├── Users
│     └── Roles
│
├── CRM
│     ├── Customers
│     └── Contacts
│
├── Catalog
│     └── Products
│
├── Inventory
│     └── Inventory Items
│
├── Sales
│     └── Sales Orders
│
└── Finance
      └── Invoices

⸻

Common Misconceptions

Incorrect Assumption	Correct Understanding
Tenant and Organization are the same	Tenant provides data isolation; Organization represents the business.
User and Customer are the same	Users operate OdinSync; Customers purchase from the organization.
Contact can log in	Contacts are CRM records only.
Every Employee is a User	Employees may or may not receive a login account.
Roles and Permissions are identical	Roles group permissions; permissions represent individual capabilities.

⸻

Summary

The OdinSync domain model separates authentication, business identity, CRM, catalog, inventory, sales, and finance into distinct concepts. This separation keeps the architecture aligned with Domain-Driven Design principles, avoids mixing unrelated responsibilities, and provides a scalable foundation for future modules while maintaining strong tenant isolation across the platform.