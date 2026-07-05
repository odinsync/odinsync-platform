# Identity & Access Schema

This document defines the database schema for the Identity & Access bounded context.

## Tables

### tenants

Represents a company/account using OdinSync.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR(150) | Tenant display name |
| status | VARCHAR(30) | ACTIVE, SUSPENDED, DELETED |
| plan | VARCHAR(50) | FREE, STARTER, PROFESSIONAL, ENTERPRISE |
| created_at | TIMESTAMP | Created timestamp |
| updated_at | TIMESTAMP | Updated timestamp |

---

### organizations

Represents the business profile of a tenant.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| tenant_id | UUID | FK to tenants.id |
| name | VARCHAR(150) | Organization name |
| legal_name | VARCHAR(200) | Legal company name |
| gst_number | VARCHAR(30) | Optional GST number |
| email | VARCHAR(150) | Business email |
| phone | VARCHAR(30) | Business phone |
| address | TEXT | Business address |
| created_at | TIMESTAMP | Created timestamp |
| updated_at | TIMESTAMP | Updated timestamp |

---

### users

Represents a user who can access OdinSync.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| tenant_id | UUID | FK to tenants.id |
| full_name | VARCHAR(150) | User full name |
| email | VARCHAR(150) | Unique login email |
| password_hash | VARCHAR(255) | BCrypt password hash |
| status | VARCHAR(30) | ACTIVE, INVITED, DISABLED |
| created_at | TIMESTAMP | Created timestamp |
| updated_at | TIMESTAMP | Updated timestamp |

Rules:

- Store only `password_hash`, never raw password.
- Initially, `email` is globally unique.
- Disabled users cannot login.

---

### roles

Represents a role inside a tenant.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| tenant_id | UUID | FK to tenants.id |
| name | VARCHAR(100) | OWNER, ADMIN, SALES_MANAGER, INVENTORY_MANAGER, ACCOUNTANT |
| description | VARCHAR(255) | Role description |
| created_at | TIMESTAMP | Created timestamp |

Rules:

- Role name should be unique within a tenant.

---

### permissions

Represents system-level permissions.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| code | VARCHAR(100) | Permission code |
| description | VARCHAR(255) | Permission description |

Rules:

- Permissions are global system metadata.
- Permissions do not belong to a tenant.

---

### role_permissions

Maps roles to permissions.

| Column | Type | Notes |
|---|---|---|
| role_id | UUID | FK to roles.id |
| permission_id | UUID | FK to permissions.id |

Rules:

- Composite primary key: `role_id`, `permission_id`.

---

### user_roles

Maps users to roles.

| Column | Type | Notes |
|---|---|---|
| user_id | UUID | FK to users.id |
| role_id | UUID | FK to roles.id |

Rules:

- Composite primary key: `user_id`, `role_id`.

---

### refresh_tokens

Stores refresh tokens for long-lived sessions.

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| tenant_id | UUID | FK to tenants.id |
| user_id | UUID | FK to users.id |
| token_hash | VARCHAR(255) | Hash of refresh token |
| expires_at | TIMESTAMP | Expiry timestamp |
| revoked_at | TIMESTAMP | Nullable |
| created_at | TIMESTAMP | Created timestamp |

Rules:

- Store token hash only, never raw refresh token.
- Revoked tokens cannot be reused.
- Refresh token belongs to one user and one tenant.

---

## Multi-Tenancy Rules

- Tenant is the SaaS security boundary.
- Business data must always belong to one tenant.
- `tenant_id` must be derived from authenticated user context.
- Client requests must not be trusted for tenant ownership.
- Queries must be tenant-aware.

---

## Indexes

Recommended indexes:

```sql
CREATE UNIQUE INDEX uk_users_email ON users(email);
CREATE UNIQUE INDEX uk_roles_tenant_name ON roles(tenant_id, name);
CREATE UNIQUE INDEX uk_permissions_code ON permissions(code);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_tenant_id ON refresh_tokens(tenant_id);