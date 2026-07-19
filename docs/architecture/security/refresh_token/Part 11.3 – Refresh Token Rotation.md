Part 11.3 – Refresh Token Rotation

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document describes how OdinSync implements Refresh Token Rotation. It focuses on the runtime implementation and intentionally avoids repeating concepts already covered in earlier documents.

Related Documents

* Part 3 – Refresh Token Lifecycle
* Part 4 – Refresh Token Rotation Concepts
* Part 5 – Token Families
* Part 6 – Replay Detection
* Part 7 – Database Design
* Part 8 – JPA Entity Design
* Part 9 – Repository Design
* Part 10 – Token Generation & Hashing
* Part 11.2 – Login Session Creation

⸻

Purpose

The refresh endpoint allows an authenticated client to obtain a new access token without requiring the user to log in again.

During every successful refresh request, OdinSync:

* Validates the submitted refresh token.
* Issues a new JWT access token.
* Generates a new refresh token.
* Marks the previous refresh token as rotated.
* Persists the replacement token within the same token family.
* Returns the new token pair to the client.

Each refresh token is valid for one successful refresh request only.

⸻

Endpoint

POST /api/v1/auth/refresh

Request

{
  "refreshToken": "<opaque-refresh-token>"
}

Successful Response

{
  "accessToken": "...",
  "expiresIn": 900,
  "refreshToken": "..."
}

⸻

Rotation Workflow

Client
↓
POST /auth/refresh
↓
Hash Incoming Refresh Token
↓
Lookup Refresh Session
↓
Validate Session
↓
Issue JWT Access Token
↓
Generate New Refresh Token
↓
Mark Existing Token ROTATED
↓
Persist Replacement Token
↓
Return New Token Pair

⸻

Component Responsibilities

AuthenticationController
            │
            ▼
RefreshTokenApplicationService
            │
            ▼
RefreshTokenService
      ├───────────────┐
      ▼               ▼
RefreshTokenStore   JwtTokenIssuer

AuthenticationController

* Exposes the REST endpoint.
* Validates request payload.
* Delegates business logic.

⸻

RefreshTokenApplicationService

Coordinates the refresh operation.

Responsibilities:

* Receive refresh request.
* Invoke domain services.
* Build response DTO.

⸻

RefreshTokenService

Responsible for:

* Session validation.
* Rotation.
* Token generation.
* Session persistence.

⸻

RefreshTokenStore

Responsible only for persistence.

Responsibilities:

* Load refresh session.
* Lock session.
* Persist updates.
* Save replacement token.

⸻

JwtTokenIssuer

Responsible only for generating a new JWT access token.

⸻

Rotation Algorithm

1. Receive refresh token.
2. Hash incoming token.
3. Load refresh session.
4. Lock session.
5. Validate:
   • exists
   • active
   • not expired
   • not revoked
   • not already rotated
6. Generate new JWT.
7. Generate new refresh token.
8. Mark current token ROTATED.
9. Persist replacement token.
10. Commit transaction.
11. Return new token pair.

⸻

Database Updates

Current refresh token:

Status = ACTIVE

↓

Successful refresh

↓

Status = ROTATED

↓

Insert replacement record

Family ID = Same
Parent = Previous Token
Status = ACTIVE

The replacement token becomes the only valid refresh token for the session.

⸻

Transaction Management

The refresh operation executes within a single transaction.

@Transactional
public RefreshTokenPair rotate(...) {
    // lock session
    // validate
    // generate tokens
    // update current token
    // insert replacement
    // commit
}

If any step fails:

* transaction rolls back
* current token remains unchanged
* replacement token is not persisted

This guarantees atomic token rotation.

⸻

Error Handling

Scenario	HTTP Status
Invalid refresh token	401 Unauthorized
Expired refresh token	401 Unauthorized
Revoked refresh token	401 Unauthorized
Rotated refresh token	401 Unauthorized
Replay detected	401 Unauthorized
User disabled	403 Forbidden
Tenant disabled	403 Forbidden

Error responses should not disclose which validation failed.

⸻

Logging

Never Log

* Access token
* Refresh token
* Refresh token hash
* Authorization header
* Cookies

Log

* User ID
* Tenant ID
* Session ID
* Token Family ID
* Request ID
* Client IP
* User Agent
* Rotation outcome

This information supports auditing without exposing credentials.

⸻

Runtime Sequence

Client
   │
POST /auth/refresh
   │
   ▼
AuthenticationController
   │
   ▼
RefreshTokenApplicationService
   │
   ▼
RefreshTokenService
   │
Hash Token
   │
Find Session
   │
Validate
   │
Generate JWT
   │
Generate Refresh Token
   │
Update Database
   │
Commit
   │
Return Token Pair
   │
   ▼
Client

⸻

Testing

The implementation should cover:

Unit Tests

* Successful rotation
* Invalid token
* Expired token
* Revoked token
* Rotated token
* Replay detection
* JWT generation failure
* Persistence failure

⸻

Integration Tests

* Refresh endpoint
* Database persistence
* Transaction rollback
* Token family continuity
* Multi-device sessions

⸻

Concurrency Tests

Verify that concurrent refresh requests cannot produce multiple active refresh tokens for the same session.

⸻

Design Principles

The OdinSync implementation follows these principles:

* Single-use refresh tokens.
* One active refresh token per session.
* Atomic token rotation.
* Stateless JWT access tokens.
* Stateful refresh sessions.
* Hash-only token persistence.
* Independent device sessions.
* Transactional consistency.
* Production-ready audit logging.

⸻

Summary

Refresh Token Rotation is the core runtime operation that maintains authenticated user sessions in OdinSync. Each successful refresh request atomically replaces the current refresh token with a newly issued one while generating a fresh JWT access token. By combining transactional updates, single-use refresh tokens, and secure hash-based persistence, OdinSync ensures that only one active refresh token exists for a session at any point in time, providing a robust foundation for replay detection, session revocation, and secure multi-device authentication.