# OdinSync Requirements

## 1. MVP Goal

Build a multi-tenant SaaS platform where a company can register, manage customers, products, inventory, sales orders, invoices, and payments.

## 2. User Roles

- Platform Admin

- Tenant Owner

- Admin

- Sales Manager

- Inventory Manager

- Accountant

- Employee

## 3. MVP Modules

- Identity & Access

- Organization

- CRM

- Catalog

- Inventory

- Sales

- Finance

## 4. Core Workflow

Company registers  

→ User logs in  

→ Customer is created  

→ Product is created  

→ Stock is added  

→ Sales order is created  

→ Inventory is reserved  

→ Invoice is generated  

→ Payment is recorded

## 5. Functional Requirements

### Identity & Access

- User can register an organization.

- User can login.

- System issues JWT token.

- User can access only their tenant data.

- Admin can invite employees.

- Admin can assign roles.

### Organization

- Tenant owner can create organization profile.

- Tenant owner can update business details.

- Organization must belong to one tenant.

### CRM

- User can create customer.

- User can update customer.

- User can search customer.

- Customer must belong to one tenant.

### Catalog

- User can create product.

- User can create category.

- User can update product price.

- Product must belong to one tenant.

### Inventory

- User can add stock.

- User can view available stock.

- System can reserve stock for order.

- System can release reserved stock if order is cancelled.

### Sales

- User can create sales order.

- Sales order contains one or more order items.

- Sales order must check inventory before confirmation.

### Finance

- System can generate invoice from sales order.

- User can record payment.

- Invoice can be marked as paid or partially paid.