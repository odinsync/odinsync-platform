Part 11.6 – Logout All Devices

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document describes how OdinSync revokes all active sessions belonging to a user.

Unlike the standard logout operation, which revokes only the current session, Logout All Devices invalidates every active refresh token issued to the user across browsers, mobile devices, tablets, and desktop applications.

This capability is typically used when:

* A user suspects account compromise.
* A password has been changed.
* An administrator disables a user account.
* A high-risk security event is detected.

Related Documents

* Part 11.2 – Login Session Creation
* Part 11.3 – Refresh Token Rotation
* Part 11.4 – Replay Detection
* Part 11.5 – Logout & Session Revocation

⸻

Purpose

The objective is to terminate every active login session associated with a user while preserving session history for auditing.

After completion:

* No refresh token issued to the user can be used again.
* Existing JWT access tokens expire naturally.
* New access tokens cannot be obtained without logging in again.

⸻

Endpoint

POST /api/v1/auth/logout-all

The endpoint requires an authenticated user.

Only the authenticated user (or an administrator) can initiate global session revocation.

⸻

Logout All Workflow

Client
↓
POST /auth/logout-all
↓
AuthenticationController
↓
LogoutAllApplicationService
↓
RefreshTokenService
↓
Find Active Sessions
↓
Revoke Active Sessions
↓
Commit Transaction
↓
Return 204 No Content

⸻

Service Responsibilities

AuthenticationController

Responsible for:

* exposing the endpoint
* validating the request
* authorizing the caller
* delegating business logic

⸻

LogoutAllApplicationService

Coordinates the workflow.

Responsibilities:

* identify the authenticated user
* invoke session revocation
* build the HTTP response

⸻

RefreshTokenService

Responsible for:

* locating active sessions
* revoking all active refresh tokens
* publishing audit events

⸻

RefreshTokenStore

Responsible only for persistence.

Typical operations:

* find active sessions by user
* bulk update session status
* commit transaction

⸻

Revocation Algorithm

Identify User
↓
Load Active Sessions
↓
For Each Active Session
↓
Status = REVOKED
↓
RevokedAt = Current Time
↓
Commit Transaction
↓
Return Success

Only sessions in the ACTIVE state are updated.

Previously revoked, rotated, or expired sessions remain unchanged.

⸻

Database Update

Example:

Before:

Session	Status
Laptop	ACTIVE
Mobile	ACTIVE
Tablet	ACTIVE
Browser	ACTIVE

After:

Session	Status
Laptop	REVOKED
Mobile	REVOKED
Tablet	REVOKED
Browser	REVOKED

Historical records are preserved.

⸻

Transaction Management

Global logout executes within a single transaction.

@Transactional
public void logoutAll(UUID userId) {
    // load active sessions
    // revoke each session
    // commit
}

If the transaction fails:

* no sessions are revoked
* database changes roll back
* client receives an error

⸻

Password Change Integration

When a user changes their password, OdinSync should invalidate all existing sessions.

Recommended sequence:

Validate Current Password
↓
Update Password
↓
Logout All Devices
↓
Require Login Again

This prevents previously issued refresh tokens from remaining usable after a password update.

⸻

Account Disable Integration

If an account is disabled:

1. Disable the user account.
2. Revoke all active sessions.
3. Reject future authentication attempts.

This ensures no active session survives account deactivation.

⸻

Administrative Session Revocation

Administrators may revoke all sessions for a user during:

* security investigations
* employee offboarding
* account compromise
* policy enforcement

The operation should be protected by administrative authorization.

⸻

JWT Behaviour

Revoking refresh sessions does not invalidate existing JWT access tokens.

Access tokens:

* remain valid until expiration
* cannot be refreshed
* naturally expire after their configured lifetime

Short-lived JWTs minimize the exposure window.

⸻

Error Handling

Scenario	HTTP Status
Sessions revoked	204 No Content
User not found	404 Not Found
Unauthorized caller	403 Forbidden
Authentication required	401 Unauthorized

Repeated requests are safe and should be treated as idempotent.

⸻

Logging

Record:

* User ID
* Tenant ID
* Number of revoked sessions
* Request ID
* Client IP
* Timestamp

Never log:

* Refresh tokens
* JWTs
* Token hashes

⸻

Performance Considerations

For users with multiple devices:

* Use bulk update operations where possible.
* Avoid loading unnecessary entity data.
* Execute within a single transaction.
* Index active sessions by user and tenant for efficient lookups.

⸻

Testing

The implementation should verify:

* revoking multiple sessions
* no active sessions present
* transaction rollback
* password change integration
* administrator-initiated logout
* disabled account workflow
* idempotent repeated requests

⸻

Design Principles

The OdinSync implementation follows these principles:

* Preserve audit history.
* Revoke instead of delete.
* Independent session management.
* Transactional consistency.
* Idempotent operations.
* Administrative support.
* Secure password-change workflow.

⸻

Future Enhancement

When OdinSync introduces authorization_version validation (planned future enhancement), the logout-all operation should also increment the user’s authorization version.

This allows protected APIs to reject previously issued access tokens before they naturally expire, providing immediate authorization revocation in addition to refresh token invalidation.

⸻

Summary

Logout All Devices provides a secure mechanism for terminating every active authenticated session belonging to a user. By revoking all refresh sessions while preserving audit history, OdinSync enables users and administrators to quickly contain compromised accounts, enforce password changes, and terminate access across all devices. Combined with short-lived JWT access tokens and the planned authorization_version enhancement, this approach delivers comprehensive session lifecycle management.