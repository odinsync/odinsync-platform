# Identity & Access

## Purpose

Identity & Access manages authentication, authorization, users, roles, permissions, and tenant ownership.

## Responsibilities

- Register tenant
- Create organization
- Create owner user
- Authenticate user
- Issue JWT token
- Manage roles
- Enforce tenant isolation

## First MVP Flow

Register Organization
→ Tenant Created
→ Organization Created
→ Owner User Created
→ OWNER Role Assigned

## Login Flow

User submits email and password
→ System validates credentials
→ System loads tenant and roles
→ JWT is generated
→ JWT returned to client

## Core Entities

- Tenant
- Organization
- User
- Role
- Permission
- RefreshToken

## Public APIs

POST /api/v1/auth/register  
POST /api/v1/auth/login  
POST /api/v1/auth/refresh-token

## Security Rules

- Passwords must be hashed using BCrypt.
- JWT must contain userId, tenantId, and roles.
- Client must never send trusted tenant_id.
- Tenant context must come from authenticated user.
- Disabled users cannot login.
- Email must be unique.

## Future Enhancements

- Email verification
- Password reset
- MFA
- Account lockout
- Audit logs