Part 11.5 – Logout & Session Revocation

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document describes how OdinSync terminates an authenticated session when a user logs out.

Logout revokes the current refresh token so it cannot be used to obtain new access tokens. Since JWT access tokens are stateless, they remain valid until they expire naturally unless additional token revocation mechanisms are implemented.

This document focuses on the implementation of session revocation.

Related Documents

* Part 11.2 – Login Session Creation
* Part 11.3 – Refresh Token Rotation
* Part 11.4 – Replay Detection

⸻

Purpose

The logout operation ensures that the current authenticated session can no longer be refreshed.

Objectives:

* Revoke the current session.
* Prevent future refresh requests.
* Preserve audit history.
* Avoid deleting session records.
* Support independent logout for multiple devices.

⸻

Endpoint

POST /api/v1/auth/logout

The endpoint requires a valid authenticated user.

The refresh token may be supplied:

* via an HttpOnly cookie (recommended for browsers), or
* in the request body for native/mobile clients.

Example request:

{
  "refreshToken": "<opaque-refresh-token>"
}

⸻

Logout Workflow

Client
↓
POST /auth/logout
↓
AuthenticationController
↓
LogoutApplicationService
↓
RefreshTokenService
↓
Hash Refresh Token
↓
Find Refresh Session
↓
Validate Session
↓
Mark Session REVOKED
↓
Commit Transaction
↓
Return 204 No Content

⸻

Service Responsibilities

AuthenticationController

Responsible for:

* exposing the logout endpoint
* validating the request
* delegating business logic

⸻

LogoutApplicationService

Coordinates the logout workflow.

Responsibilities:

* invoke refresh token revocation
* build the HTTP response

⸻

RefreshTokenService

Responsible for:

* locating the session
* validating ownership
* revoking the refresh token
* publishing audit events

⸻

RefreshTokenStore

Responsible only for persistence.

Responsibilities:

* load session
* update status
* commit transaction

⸻

Revocation Algorithm

Receive Refresh Token
↓
Hash Token
↓
Find Refresh Session
↓
Validate Ownership
↓
Validate Status
↓
Set Status = REVOKED
↓
Update Revoked Timestamp
↓
Commit
↓
Return Success

⸻

Database Updates

Before logout:

Status = ACTIVE

After logout:

Status = REVOKED
RevokedAt = Current Timestamp

The record is retained for:

* audit history
* replay detection
* security investigations

Refresh sessions should not be physically deleted during logout.

⸻

JWT Behaviour

Logout does not invalidate previously issued JWT access tokens.

Because JWTs are stateless:

* they cannot be removed from clients
* they remain valid until expiration
* they cannot obtain new access tokens after the refresh token has been revoked

To reduce exposure:

* keep access tokens short-lived (for example, 15 minutes)
* use refresh token revocation for long-term session control

⸻

Transaction Management

Logout executes within a single transaction.

@Transactional
public void logout(String refreshToken) {
    // hash token
    // load session
    // validate
    // revoke session
    // commit
}

If the transaction fails:

* no revocation is persisted
* session remains active
* client receives an error response

⸻

Browser Logout

Recommended flow:

1. Revoke refresh session.
2. Expire the HttpOnly refresh token cookie.
3. Remove access token from client memory.
4. Redirect to the login page if appropriate.

⸻

Native Client Logout

Recommended flow:

1. Call the logout endpoint.
2. Delete refresh token from secure storage.
3. Delete access token from memory.
4. Clear cached authentication state.

⸻

Error Handling

Scenario	HTTP Status
Session revoked successfully	204 No Content
Invalid refresh token	401 Unauthorized
Refresh token expired	401 Unauthorized
Refresh token already revoked	204 No Content
Session not found	401 Unauthorized

Treating repeated logout requests as successful (idempotent) simplifies client implementations and avoids unnecessary errors.

⸻

Logging

Record:

* User ID
* Tenant ID
* Session ID
* Family ID
* Client IP
* User Agent
* Logout timestamp

Never log:

* JWT
* Refresh token
* Refresh token hash

⸻

Testing

The logout implementation should verify:

* successful logout
* invalid refresh token
* expired session
* already revoked session
* transaction rollback
* audit event publication
* browser cookie removal
* native client logout flow

⸻

Design Principles

The OdinSync logout implementation follows these principles:

* Session revocation instead of deletion.
* Idempotent logout operations.
* Stateless JWT access tokens.
* Independent device logout.
* Complete audit history.
* Transactional consistency.

⸻

Summary

Logout in OdinSync revokes the current refresh session while preserving session history for auditing and security analysis. By retaining revoked sessions rather than deleting them, the system continues to support replay detection and forensic investigation. Combined with short-lived JWT access tokens, this approach provides secure and predictable session termination without introducing server-side state for access tokens.