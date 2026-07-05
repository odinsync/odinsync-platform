# Database Schema

OdinSync uses a shared database and shared schema multi-tenant model.

Every business table must contain `tenant_id`.

## Core Tables

### tenants

Stores tenant/account information.

Columns:
- id
- name
- status
- plan
- created_at
- updated_at

---

### organizations

Stores company profile.

Columns:
- id
- tenant_id
- name
- legal_name
- gst_number
- email
- phone
- address
- created_at
- updated_at

---

### users

Stores platform users.

Columns:
- id
- tenant_id
- full_name
- email
- password_hash
- status
- created_at
- updated_at

Rules:
- email must be unique.
- user belongs to one tenant.

---

### roles

Columns:
- id
- tenant_id
- name
- description

---

### user_roles

Columns:
- user_id
- role_id

---

### customers

Columns:
- id
- tenant_id
- name
- email
- phone
- billing_address
- shipping_address
- status
- created_at
- updated_at

---

### categories

Columns:
- id
- tenant_id
- name
- description

---

### products

Columns:
- id
- tenant_id
- category_id
- sku
- name
- description
- selling_price
- status
- created_at
- updated_at

Rules:
- sku must be unique within tenant.

---

### warehouses

Columns:
- id
- tenant_id
- name
- location
- status

---

### inventory_items

Columns:
- id
- tenant_id
- product_id
- warehouse_id
- available_quantity
- reserved_quantity
- created_at
- updated_at

Rules:
- available_quantity >= 0
- reserved_quantity >= 0

---

### sales_orders

Columns:
- id
- tenant_id
- customer_id
- order_number
- status
- total_amount
- created_at
- updated_at

---

### sales_order_items

Columns:
- id
- tenant_id
- sales_order_id
- product_id
- quantity
- unit_price
- line_total

---

### invoices

Columns:
- id
- tenant_id
- sales_order_id
- invoice_number
- status
- total_amount
- paid_amount
- created_at
- updated_at

---

### payments

Columns:
- id
- tenant_id
- invoice_id
- amount
- payment_mode
- payment_date
- created_at

---

### audit_logs

Columns:
- id
- tenant_id
- actor_user_id
- action
- entity_type
- entity_id
- created_at