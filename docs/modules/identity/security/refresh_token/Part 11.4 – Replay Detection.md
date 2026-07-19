Part 11.4 – Replay Detection

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document describes how OdinSync detects and responds to refresh token replay attacks.

The concepts behind replay attacks and token families are covered in previous documentation. This document focuses on the runtime implementation.

Related Documents

* Part 5 – Token Families
* Part 6 – Replay Detection Concepts
* Part 11.3 – Refresh Token Rotation

⸻

Purpose

A refresh token must be used exactly once.

If a refresh token that has already been rotated is submitted again, OdinSync treats the request as a potential credential compromise rather than a normal authentication failure.

The objective is to:

* Prevent reuse of stolen refresh tokens.
* Detect concurrent refresh attempts.
* Revoke compromised sessions.
* Record security events for auditing.

⸻

Detection Flow

Client
    │
Submit Refresh Token
    │
    ▼
Hash Token
    │
    ▼
Lookup Refresh Session
    │
    ▼
Validate Session State
    │
    ├── ACTIVE  ─────────────► Continue Rotation
    │
    ├── ROTATED ─────────────► Replay Detected
    │
    ├── REVOKED ─────────────► Reject Request
    │
    └── EXPIRED ─────────────► Reject Request

⸻

Replay Detection Rules

A replay attack is detected when:

* A rotated refresh token is reused.
* A revoked refresh token is reused.
* Multiple requests attempt to refresh the same token simultaneously.
* A previously invalidated session attempts another refresh.

⸻

Runtime Behaviour

When replay is detected, OdinSync performs the following steps:

1. Stop request processing.
2. Do not issue new tokens.
3. Revoke all active tokens in the same token family.
4. Persist the revocation.
5. Record a security audit event.
6. Return 401 Unauthorized.

⸻

Token Family Revocation

Family F1
Token A (ROTATED)
↓
Token B (ACTIVE)
↓
Replay of Token A
↓
Family Revoked
↓
Token B → REVOKED

After revocation, the user must authenticate again.

⸻

Transaction Flow

Lock Refresh Session
↓
Validate Status
↓
Replay Detected
↓
Revoke Family
↓
Commit
↓
Return 401

All updates occur within a single transaction.

⸻

Logging

Record:

* User ID
* Tenant ID
* Family ID
* Session ID
* Client IP
* User Agent
* Timestamp
* Replay detection result

Never log:

* JWT
* Refresh token
* Token hash

⸻

API Response

HTTP/1.1 401 Unauthorized

Example response:

{
  "code": "REFRESH_TOKEN_REPLAY_DETECTED",
  "message": "Authentication required."
}

The response should not reveal whether the token was expired, rotated, or revoked.

⸻

Testing

The replay detection implementation should verify:

* Reuse of a rotated token.
* Reuse of a revoked token.
* Concurrent refresh requests.
* Token family revocation.
* Transaction rollback on failure.
* Audit event generation.

⸻

Summary

Replay detection is the final security layer protecting OdinSync’s refresh token mechanism. By treating reuse of a refresh token as a potential compromise, revoking the entire token family, and requiring re-authentication, OdinSync minimizes the impact of stolen credentials while maintaining a simple and predictable session model.