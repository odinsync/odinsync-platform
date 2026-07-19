# Refresh Token Rotation
## Part 2 – JWT vs Refresh Token

> **Project:** OdinSync  
> **Module:** Identity & Access Management  
> **Document:** Refresh Token Rotation  
> **Part:** 2 – JWT vs Refresh Token

---

# Overview

In the previous chapter, we discussed why refresh tokens are required and the security goals they help achieve.

This chapter explains the two-token authentication model used by OdinSync:

- **Access Token (JWT)** – Used to access protected APIs.
- **Refresh Token** – Used to obtain a new access token after the current one expires.

Although both tokens are issued during login, they serve different purposes and have different security characteristics.

---

# Why Two Tokens?

A common question is:

> Why not issue a single JWT with a long expiration time?

While this simplifies implementation, it creates significant security risks.

Example:

```
User Login
      │
      ▼
Generate JWT
      │
      ▼
Valid for 30 Days
```

If this JWT is stolen, the attacker can access protected APIs until it expires because the server validates the token without consulting the database.

Instead, OdinSync separates authentication into two tokens:

```
                Login
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
 Access Token         Refresh Token
     (JWT)               (Opaque)
```

This provides the best balance between security, scalability, and user experience.

---

# Access Token (JWT)

An Access Token is a **JSON Web Token (JWT)** that contains information required to authorize requests.

It is sent with every protected API request.

Example:

```
GET /api/customers

Authorization: Bearer eyJhbGciOi...
```

Spring Security validates the token by:

1. Verifying the digital signature.
2. Checking the expiration time.
3. Reading the JWT claims.
4. Creating an Authentication object.
5. Populating the SecurityContext.

No database lookup is required.

---

# Typical JWT Claims

A JWT may contain claims similar to:

```json
{
  "sub": "user123",
  "email": "john@example.com",
  "tenant": "tenant1",
  "roles": [
    "ADMIN",
    "USER"
  ],
  "iat": 1712345678,
  "exp": 1712346578
}
```

These claims allow Spring Security to authorize requests without querying the database.

---

# Characteristics of Access Tokens

| Property | Value |
|----------|-------|
| Format | JWT |
| Stored by Client | Yes |
| Sent with Every API | Yes |
| Contains Claims | Yes |
| Database Lookup | No |
| Signed | Yes |
| Short-Lived | Yes |

Typical lifetime:

```
10–15 Minutes
```

---

# Why Are Access Tokens Short-Lived?

Since JWT validation is stateless, the server cannot immediately revoke an issued JWT.

Example:

```
JWT Issued

↓

JWT Stolen

↓

Signature Valid

↓

Expiration Not Reached

↓

Access Granted
```

Short expiration reduces the window in which a stolen token can be abused.

---

# Refresh Token

A Refresh Token is used only to request a new access token.

Unlike a JWT, it is **never** used to access protected APIs.

Example:

```
POST /auth/refresh

{
    "refreshToken": "4NMQj8w7..."
}
```

If valid, the server generates:

- New Access Token
- New Refresh Token (Rotation)

---

# Characteristics of Refresh Tokens

| Property | Value |
|----------|-------|
| Format | Opaque Random String |
| Contains Claims | No |
| Used for Protected APIs | No |
| Database Lookup | Yes |
| Rotated | Yes |
| Revocable | Yes |
| Long-Lived | Yes |

Typical lifetime:

```
30 Days
```

---

# JWT vs Refresh Token

| Feature | Access Token | Refresh Token |
|----------|-------------|---------------|
| Format | JWT | Random String |
| Contains Claims | Yes | No |
| Used for APIs | Yes | No |
| Used to Refresh | No | Yes |
| Stateless Validation | Yes | No |
| Database Lookup | No | Yes |
| Lifetime | Short | Long |
| Rotated | No | Yes |

---

# Why OdinSync Uses Opaque Refresh Tokens

There are two common approaches:

### Option 1 – JWT Refresh Token

```
Refresh Token

↓

JWT

↓

Contains Claims
```

### Option 2 – Opaque Refresh Token

```
Random String

↓

Database Lookup

↓

Validate Session
```

OdinSync chooses **opaque refresh tokens** because they provide better session management.

Benefits include:

- Immediate revocation
- Rotation support
- Replay detection
- Device tracking
- Logout support
- Audit history
- Better control over active sessions

---

# Why Not Use JWT Refresh Tokens?

A JWT refresh token is also self-contained.

Although it can be verified without a database lookup, this removes server-side control.

Problems include:

- Difficult to revoke immediately
- Harder to detect replay attacks
- No easy session tracking
- Complicated logout implementation

For a multi-tenant SaaS platform like OdinSync, maintaining session state on the server is a better design choice.

---

# Why Store Refresh Tokens in the Database?

Refresh tokens represent authenticated user sessions.

Persisting them allows OdinSync to:

- Revoke sessions
- Detect reused tokens
- Track login devices
- Enforce expiration
- Support logout
- Audit active sessions

A simplified table looks like:

| Column | Purpose |
|---------|----------|
| id | Primary Key |
| user_id | User |
| tenant_id | Tenant |
| token_hash | Hashed Token |
| family_id | Token Family |
| expires_at | Expiration |
| revoked_at | Revocation Time |
| replaced_by | Next Token |

The raw refresh token is **never stored** in the database.

Only its cryptographic hash is persisted.

---

# Authentication Flow

```
                Login
                  │
                  ▼
      Verify Username/Password
                  │
      ┌───────────┴───────────┐
      ▼                       ▼
 Generate JWT         Generate Refresh Token
      │                       │
      ▼                       ▼
 Return to Client      Store Hash in Database
```

---

# Refresh Flow

```
Access Token Expired
          │
          ▼
Client Calls /auth/refresh
          │
          ▼
Validate Refresh Token
          │
          ▼
Generate New JWT
          │
          ▼
Rotate Refresh Token
          │
          ▼
Return New Token Pair
```

---

# Security Responsibilities

## Access Token

Responsible for:

- Authentication
- Authorization
- API access
- Carrying claims

It should **never** be used to maintain long-running sessions.

---

## Refresh Token

Responsible for:

- Session continuation
- Session validation
- Token rotation
- Session revocation
- Replay detection

It should **never** be accepted by protected APIs.

---

# Design Decisions in OdinSync

The refresh-token implementation follows these principles:

- JWTs remain short-lived.
- Refresh tokens are opaque random values.
- Refresh tokens are stored as hashes.
- Every refresh request validates server-side state.
- Refresh tokens are rotated after successful use.
- Refresh tokens are single-use.
- Protected APIs remain stateless.
- Refresh operations may query the database.

These decisions balance performance, scalability, and security.

---

# Summary

OdinSync uses a two-token authentication model to separate API authorization from session management.

**Access Token**

- JWT
- Short-lived
- Stateless validation
- Sent with every protected request

**Refresh Token**

- Opaque random string
- Long-lived
- Stored as a hash in the database
- Used only for `/auth/refresh`
- Rotated after every successful use
- Supports revocation and replay detection

In the next chapter, we will explore the **Refresh Token Lifecycle**, covering login, token issuance, expiration, rotation, and logout preparation in detail.