# Refresh Token Rotation
## Part 1 – Introduction & Security Goals

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 1 – Introduction & Security Goals
>
> **Status:** Design Document

---

# Document Overview

This document explains the complete refresh-token architecture used by OdinSync.

Unlike the login flow documentation, which focuses on user authentication and JWT generation, this document explains how OdinSync maintains secure long-running user sessions while minimizing security risks.

The document not only explains **how refresh tokens are implemented**, but also **why each design decision was made**, including the security, scalability, and maintainability trade-offs.

The goal is to make this document useful for:

- Future OdinSync contributors
- Architecture discussions
- Security reviews
- Production maintenance
- Interview preparation
- Understanding Spring Security internals

---

# Target Audience

This document assumes the reader is familiar with:

- Java
- Spring Boot
- Spring Security
- JWT
- OAuth2 Resource Server
- REST APIs

Prior knowledge of refresh-token rotation is **not required**, as every concept is introduced from first principles.

---

# Related Documentation

This document builds upon the previous OdinSync security documentation.

Recommended reading order:

```
1.
login-authentication-flow.md

↓

2.
protected-api-authentication-flow.md

↓

3.
rbac-authorization-flow.md

↓

4.
refresh-token-rotation.md   ← Current document
```

Each document introduces the next phase of the authentication system.

---

# Current OdinSync Authentication Flow

At the time of writing this document, OdinSync already supports the following authentication features.

```
User Registration
        ✅

↓

Password Encryption (BCrypt)
        ✅

↓

User Login
        ✅

↓

AuthenticationManager
        ✅

↓

JWT Generation
        ✅

↓

OAuth2 Resource Server
        ✅

↓

JWT Validation
        ✅

↓

SecurityContext Population
        ✅

↓

Role Based Authorization
        ✅

↓

Protected APIs
        ✅

↓

Refresh Token Rotation
        ← Current Phase
```

The current implementation already provides secure authentication using JWT access tokens.

The next challenge is maintaining user sessions securely after the access token expires.

---

# Why This Document Exists

When the login flow was implemented, the authentication process ended after generating the JWT.

```
User Login

↓

Verify Password

↓

Generate JWT

↓

Return JWT
```

This workflow is sufficient for demonstrating authentication, but it is incomplete for a production SaaS platform.

A production application must answer additional questions such as:

- What happens when the JWT expires?
- Should the user log in again?
- How does "Remember Me" work?
- How do mobile applications stay logged in?
- How can sessions be revoked?
- How do we detect stolen refresh tokens?
- How do we terminate only one device?
- How do we support multiple logged-in devices?

The purpose of refresh tokens is to answer all of these questions securely.

---

# Security Philosophy

OdinSync follows one important security principle.

> Authentication should be easy for legitimate users and difficult for attackers.

This means:

- Users should not log in repeatedly.
- Access tokens should expire quickly.
- Stolen credentials should become useless as quickly as possible.
- Sessions should be revocable.
- Every authentication decision should originate from trusted server-side state.

---

# Why Short-Lived Access Tokens?

JWT access tokens are designed for speed.

When a protected API receives a JWT, Spring Security performs the following steps.

```
Receive JWT

↓

Verify Signature

↓

Verify Expiration

↓

Read Claims

↓

Create Authentication

↓

SecurityContext
```

Notice something important.

No database query occurs.

This is intentional.

Stateless validation provides:

- Excellent performance
- Horizontal scalability
- No session lookup
- Reduced database load

Unfortunately, stateless validation introduces a limitation.

If a JWT remains valid for a long time, the server has no opportunity to revoke it before it expires.

Example:

```
Access Token Lifetime

30 Days
```

Suppose the token is stolen.

```
Attacker

↓

Obtains JWT

↓

JWT Signature Valid

↓

JWT Not Expired

↓

Protected API

↓

Access Granted
```

The attacker may continue using the token for the remaining lifetime.

Therefore, access tokens should always remain short-lived.

Typical production values:

| Token | Recommended Lifetime |
|---------|---------------------|
| Access Token | 10–15 minutes |
| Refresh Token | 30 days |

OdinSync follows this same approach.

---

# The User Experience Problem

Short-lived access tokens improve security.

However, they introduce another problem.

Imagine an access token expires every 15 minutes.

```
Login

↓

15 Minutes

↓

Login Again

↓

15 Minutes

↓

Login Again

↓

15 Minutes

↓

Login Again
```

This would create an unacceptable user experience.

Users expect applications to remember them.

Examples include:

- Gmail
- GitHub
- Slack
- Microsoft Teams
- Notion
- Jira

These applications do not ask users to log in every few minutes.

Instead, they silently obtain a new access token.

Refresh tokens make this possible.

---

# What Problem Does a Refresh Token Solve?

A refresh token separates two responsibilities.

The access token answers:

> Can this request access a protected API?

The refresh token answers:

> Is this user still allowed to receive another access token?

This separation allows OdinSync to use:

```
Short-Lived JWT

+

Long-Lived Refresh Token
```

The access token remains optimized for performance.

The refresh token remains optimized for security and session management.

---

# High-Level Authentication Architecture

```
                    Login

                     │

                     ▼

         AuthenticationManager

                     │

                     ▼

          Verify Username/Password

                     │

        ┌────────────┴────────────┐

        ▼                         ▼

Generate Access Token      Generate Refresh Token

        │                         │

        ▼                         ▼

Return JWT             Store Refresh Token

        │

        ▼

Protected APIs

        │

Access Token Expires

        │

        ▼

Client Calls

/auth/refresh

        │

        ▼

Validate Refresh Token

        │

        ▼

Generate New JWT

        │

        ▼

Continue Session
```

Notice that the refresh token is **never** sent to protected APIs.

Likewise, the access token is **never** used to request another refresh token.

Each token has one clearly defined responsibility.

---

# Design Goals

The refresh-token implementation must satisfy several requirements.

## Goal 1 — Excellent User Experience

Users should remain signed in without repeatedly entering their password.

---

## Goal 2 — Short-Lived JWTs

Access tokens should remain short-lived.

This reduces the usefulness of stolen tokens.

---

## Goal 3 — Session Revocation

The server must be capable of terminating a session.

Examples include:

- Logout
- Password change
- Administrator action
- Suspicious activity

---

## Goal 4 — Device Independence

Each login should represent an independent session.

For example:

```
Laptop

↓

Independent Session

Mobile

↓

Independent Session

Tablet

↓

Independent Session
```

Logging out of one device should not automatically log out every device unless explicitly requested.

---

## Goal 5 — Replay Detection

If an old refresh token appears again, the server should recognize that something abnormal has occurred.

Examples include:

- Token theft
- Network replay
- Duplicate requests
- Stale client state

The implementation must detect these situations.

---

## Goal 6 — Server Authority

The server should remain the source of truth.

The client should never decide:

- Session validity
- Expiration
- Revocation
- Authorization

All security decisions must originate from trusted server-side data.

---

## Goal 7 — Scalability

Protected API requests occur far more frequently than refresh requests.

Therefore:

Protected APIs should remain stateless.

Refresh operations may perform a database lookup because they occur relatively infrequently.

This balance provides both scalability and security.

---

# Design Principles

Throughout this document, every implementation decision follows these principles.

### Principle 1

Keep access tokens short-lived.

---

### Principle 2

Refresh tokens represent sessions.

---

### Principle 3

Every refresh token should be usable only once.

---

### Principle 4

Security decisions originate from server-side state.

---

### Principle 5

Historical session information should never be discarded immediately.

---

### Principle 6

Security is preferred over convenience whenever a compromise is suspected.

---

# Scope of This Document

The remaining sections of this document explain:

- JWT vs Refresh Tokens
- Why refresh tokens are opaque
- Refresh-token lifecycle
- Token rotation
- Token families
- Replay detection
- Database design
- Spring implementation
- Transaction management
- Pessimistic locking
- Controller implementation
- Security configuration
- Testing strategy
- Future enhancements

Each topic builds upon the previous one so that the complete architecture can be understood from first principles.

---

# Summary

At this point we have established the motivation for refresh tokens.

We know that:

- JWTs are intentionally short-lived.
- Stateless validation provides excellent scalability.
- Long-lived JWTs increase security risk.
- Requiring frequent logins creates a poor user experience.
- Refresh tokens allow secure session continuation.
- The refresh token is responsible for maintaining session state.
- The access token remains responsible only for authorization.

In the next section, we will compare **Access Tokens and Refresh Tokens** in detail and explain why OdinSync uses **opaque refresh tokens** instead of JWT refresh tokens.