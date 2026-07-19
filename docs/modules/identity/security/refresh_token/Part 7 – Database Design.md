# Refresh Token Rotation
## Part 7 – Database Design

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 7 – Database Design

---

# Overview

A refresh token is more than a random string.

It represents an authenticated session that must support:

- Token rotation
- Session revocation
- Replay detection
- Device management
- Audit history
- Future logout support

To enable these features, OdinSync stores refresh-token metadata in the database.

Unlike JWT access tokens, refresh tokens are **stateful**.

Every refresh request consults the database before issuing a new access token.

---

# Why Store Refresh Tokens?

Access tokens are validated without consulting the database.

```
Receive JWT

↓

Verify Signature

↓

Verify Expiration

↓

Access Granted
```

Refresh tokens behave differently.

```
Receive Refresh Token

↓

Hash Token

↓

Find Session

↓

Validate Session

↓

Generate New Tokens
```

Because refresh tokens represent server-controlled sessions, their state must be persisted.

---

# Refresh Token Table

The table below represents the proposed schema.

| Column | Description |
|---------|-------------|
| id | Primary key |
| user_id | User who owns the session |
| tenant_id | Tenant/Organization |
| token_hash | SHA-256 hash of the refresh token |
| family_id | Groups all tokens in the same session |
| replaced_by_token_id | Points to the next refresh token |
| issued_at | Time token was issued |
| expires_at | Expiration timestamp |
| revoked_at | Revocation timestamp |
| created_at | Audit timestamp |
| updated_at | Audit timestamp |

---

# Table Structure

```
refresh_tokens
│
├── id
├── user_id
├── tenant_id
├── token_hash
├── family_id
├── replaced_by_token_id
├── issued_at
├── expires_at
├── revoked_at
├── created_at
└── updated_at
```

Each field exists for a specific reason.

---

# Primary Key (`id`)

Every refresh token has a unique identifier.

```java
UUID id;
```

This identifier represents a single refresh token.

Example:

```
R1

↓

UUID

↓

550e8400-e29b-41d4-a716-446655440000
```

Using UUIDs avoids predictable identifiers and supports distributed deployments.

---

# User ID

```java
UUID userId;
```

Identifies the authenticated user.

Example:

```
Refresh Token

↓

User 123
```

This relationship allows the server to:

- Find all sessions for a user.
- Revoke all user sessions.
- Audit authentication history.

---

# Tenant ID

```java
UUID tenantId;
```

OdinSync is a multi-tenant platform.

Every refresh token belongs to a tenant.

```
Tenant A

↓

User

↓

Refresh Token
```

This prevents cross-tenant access and simplifies tenant-scoped administration.

---

# Token Hash

```java
String tokenHash;
```

The raw refresh token is **never stored**.

Instead:

```
Random Token

↓

SHA-256

↓

Database
```

Example:

```
Raw Token

↓

A9FkQe82...

↓

SHA-256

↓

0f8d91e2...
```

If the database is compromised, attackers cannot directly use stored values as refresh tokens.

---

# Why Not Store the Raw Token?

Suppose the database is leaked.

If raw refresh tokens are stored:

```
Database Leak

↓

Attacker Reads Token

↓

Uses Token
```

By storing only the hash:

```
Database Leak

↓

Attacker Gets Hash

↓

Cannot Authenticate
```

This follows the same principle as password storage.

---

# Family ID

```java
UUID familyId;
```

Every login creates a new token family.

```
Family

↓

R1

↓

R2

↓

R3
```

The value never changes throughout the lifetime of the session.

This enables:

- Session revocation
- Replay detection
- Device tracking

---

# Replaced By Token ID

```java
UUID replacedByTokenId;
```

Records which refresh token replaced the current one.

```
R1

↓

R2

↓

R3
```

Database example:

| Token | Replaced By |
|--------|-------------|
| R1 | R2 |
| R2 | R3 |
| R3 | NULL |

This preserves the rotation history.

---

# Issued At

```java
Instant issuedAt;
```

Records when the refresh token was created.

Useful for:

- Auditing
- Analytics
- Session age
- Device history

---

# Expires At

```java
Instant expiresAt;
```

Defines when the refresh token becomes invalid.

Example:

```
Issued

↓

2026-07-19

↓

Expires

↓

2026-08-18
```

Expired tokens are rejected even if they have never been used.

---

# Revoked At

```java
Instant revokedAt;
```

A non-null value indicates the token has been revoked.

```
NULL

↓

Active
```

```
2026-07-20

↓

Revoked
```

Reasons include:

- Rotation
- Logout
- Password change
- Replay detection
- Administrative revocation

---

# Audit Fields

```java
createdAt
updatedAt
```

These support:

- Operational troubleshooting
- Audit requirements
- Historical reporting

Most applications manage these fields automatically using Spring Data JPA auditing.

---

# Database Relationships

```
User

│

├──────────────┐

│              │

▼              ▼

Refresh Token  Refresh Token

│

▼

Replacement Token
```

One user may have multiple active sessions across devices.

---

# Recommended Indexes

Efficient lookups are important because refresh validation occurs frequently.

Recommended indexes:

| Index | Purpose |
|--------|----------|
| token_hash | Find refresh token quickly |
| family_id | Revoke session |
| user_id | List user sessions |
| tenant_id | Tenant isolation |
| expires_at | Cleanup jobs |

---

# Example Record

| Column | Value |
|---------|-------|
| id | 3d4a... |
| user_id | a92b... |
| tenant_id | 55fe... |
| token_hash | 7b18... |
| family_id | f218... |
| replaced_by_token_id | NULL |
| issued_at | 2026-07-19 09:30 UTC |
| expires_at | 2026-08-18 09:30 UTC |
| revoked_at | NULL |

This record represents the current active refresh token for one authenticated session.

---

# Design Decisions in OdinSync

The database design follows these principles:

- Store only hashed refresh tokens.
- Preserve complete rotation history.
- Represent each login as a token family.
- Support multiple concurrent device sessions.
- Enable efficient revocation and replay detection.
- Maintain audit information.

---

# Looking Ahead

Now that the database schema is defined, the next step is implementing it in Spring Boot.

The following chapter will cover:

- `RefreshTokenJpaEntity`
- JPA annotations
- UUID mapping
- Relationships
- Auditing
- Helper methods such as `isExpired()`, `isRevoked()`, and `isActive()`

These classes will become the foundation of the refresh token service.

---

# Summary

The refresh token table is the central source of truth for session management in OdinSync.

Unlike stateless JWTs, refresh tokens require persistent state to support:

- Secure rotation
- Replay detection
- Session revocation
- Multi-device support
- Audit history

By storing only token hashes and maintaining complete session history, OdinSync provides a secure and scalable foundation for long-lived user sessions.