# Refresh Token Rotation
## Part 3 – Refresh Token Lifecycle

> **Project:** OdinSync  
> **Module:** Identity & Access Management  
> **Document:** Refresh Token Rotation  
> **Part:** 3 – Refresh Token Lifecycle

---

# Overview

In the previous chapter, we learned the difference between an Access Token and a Refresh Token.

This chapter explains the complete lifecycle of a refresh token—from the moment a user logs in until the session ends.

Understanding this lifecycle is important because every future feature (rotation, replay detection, logout, device management, and session revocation) builds upon it.

---

# Refresh Token Lifecycle

A refresh token passes through several stages during its lifetime.

```
             Login
               │
               ▼
      Refresh Token Issued
               │
               ▼
      Stored Securely by Client
               │
               ▼
     Access Token Expires
               │
               ▼
      Refresh Request Sent
               │
               ▼
   Refresh Token Validated
               │
               ▼
     New Token Pair Issued
               │
               ▼
 Previous Refresh Token Revoked
               │
               ▼
 Continue Session
```

Eventually, the lifecycle ends when:

- the user logs out,
- the refresh token expires,
- an administrator revokes the session, or
- token reuse is detected.

---

# Phase 1 – User Login

The lifecycle begins after successful authentication.

```
Client

↓

POST /auth/login

↓

AuthenticationManager

↓

Verify Username & Password

↓

Authentication Successful
```

At this point, OdinSync knows the user's identity and permissions.

The authentication service generates two tokens:

- Access Token (JWT)
- Refresh Token (Opaque Random String)

```
                Login
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
 Access Token         Refresh Token
```

The access token is returned to the client.

The refresh token is also returned, but only its **hash** is stored in the database.

---

# Phase 2 – Refresh Token Creation

A refresh token is generated using a cryptographically secure random number generator.

Example:

```
7YvBqf2vQJ6EhP...
```

The raw token is returned only once to the client.

Before storing it, OdinSync computes its SHA-256 hash.

```
Raw Token

↓

SHA-256

↓

Hash Stored in Database
```

This protects the token if the database is compromised.

---

# Phase 3 – Client Storage

The client stores both tokens.

```
Client

├── Access Token
└── Refresh Token
```

Typical storage recommendations:

| Client | Storage |
|---------|----------|
| Web | Secure HttpOnly Cookie (recommended) |
| Mobile | Keychain / Keystore |
| Desktop | Encrypted Secure Storage |

Refresh tokens should never be stored in:

- Local Storage (web)
- Plain text files
- Application logs
- URLs

---

# Phase 4 – Accessing Protected APIs

While the access token remains valid, the refresh token is **not used**.

```
GET /customers

Authorization: Bearer <JWT>
```

Spring Security validates the JWT and authorizes the request.

```
Receive JWT

↓

Verify Signature

↓

Verify Expiration

↓

SecurityContext

↓

Controller
```

This process is completely stateless.

---

# Phase 5 – Access Token Expiration

Eventually, the access token expires.

Example:

```
Access Token

Expires After

15 Minutes
```

When the client calls a protected API:

```
GET /customers
```

Spring Security returns:

```
401 Unauthorized
```

The user does **not** need to log in again.

Instead, the client begins the refresh flow.

---

# Phase 6 – Refresh Request

The client calls:

```
POST /auth/refresh
```

Example request:

```http
POST /auth/refresh

{
    "refreshToken": "7YvBqf2vQJ6EhP..."
}
```

The refresh token is **never** sent with protected API requests.

It is only submitted to the refresh endpoint.

---

# Phase 7 – Refresh Token Validation

The server performs several validation steps.

```
Receive Refresh Token
          │
          ▼
Hash Token
          │
          ▼
Find Matching Session
          │
          ▼
Check Expiration
          │
          ▼
Check Revocation
          │
          ▼
Check Reuse
```

Only after all validations succeed does the server issue a new token pair.

---

# Phase 8 – Token Rotation

Instead of extending the existing refresh token, OdinSync issues a brand-new refresh token.

```
Old Token

↓

R1

↓

Create R2

↓

Revoke R1

↓

Return R2
```

This process is called **Refresh Token Rotation**.

The old refresh token becomes permanently invalid.

---

# Phase 9 – New Token Pair

After successful validation:

```
Old Access Token

Expired

↓

Generate New JWT

↓

Generate New Refresh Token

↓

Return Both
```

The client replaces its stored tokens.

```
Old Tokens

↓

Discard

↓

Store New Tokens
```

This process is transparent to the user.

---

# Phase 10 – Session Continuation

From the user's perspective:

```
Login Once

↓

Continue Working

↓

Tokens Refreshed Automatically

↓

No Additional Login Required
```

The session continues seamlessly while maintaining short-lived access tokens.

---

# Phase 11 – Session Termination

Eventually, the refresh token lifecycle ends.

Common scenarios include:

### Logout

```
User Clicks Logout

↓

Refresh Token Revoked

↓

Session Ends
```

---

### Token Expiration

```
Refresh Token

↓

30 Days

↓

Expired

↓

Login Required
```

---

### Administrator Revocation

```
Admin

↓

Disable Session

↓

Refresh Token Revoked
```

---

### Password Change

```
Password Changed

↓

Existing Sessions Revoked
```

---

### Replay Detection

```
Old Refresh Token Used Again

↓

Possible Token Theft

↓

Entire Token Family Revoked
```

Replay detection will be discussed in detail in the next chapters.

---

# Complete Lifecycle

```
                Login
                  │
                  ▼
      Generate Access Token
                  │
                  ▼
     Generate Refresh Token
                  │
                  ▼
 Store Refresh Token Hash
                  │
                  ▼
 Client Uses JWT
                  │
                  ▼
 JWT Expires
                  │
                  ▼
 Client Calls Refresh API
                  │
                  ▼
 Validate Refresh Token
                  │
                  ▼
 Rotate Refresh Token
                  │
                  ▼
 Return New Token Pair
                  │
                  ▼
 Continue Session
                  │
                  ▼
 Logout / Expiration /
 Replay Detection
                  │
                  ▼
 Session Ends
```

---

# Why This Lifecycle Matters

The refresh token lifecycle provides several benefits:

- Users remain logged in without frequent authentication.
- Access tokens remain short-lived, reducing security risks.
- Refresh tokens represent server-controlled sessions.
- Every successful refresh strengthens security through token rotation.
- Sessions can be revoked without affecting JWT validation performance.

This design balances user experience with strong security.

---

# Summary

A refresh token progresses through the following stages:

1. Generated after successful login.
2. Stored securely by the client.
3. Used only when the access token expires.
4. Validated against server-side state.
5. Rotated after every successful refresh.
6. Revoked when replaced.
7. Eventually expires or is revoked.

At no point is the refresh token used to access protected APIs directly.

In the next chapter, we will examine **Refresh Token Rotation** in depth, including why every refresh token is single-use, how token families work, and how OdinSync detects replay attacks.