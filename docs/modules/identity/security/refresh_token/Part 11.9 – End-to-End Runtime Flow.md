Part 11.9 – End-to-End Runtime Flow

Project: OdinSync
Module: Identity & Access Management
Component: Authentication & Refresh Token Service

⸻

Overview

This document describes the complete runtime authentication lifecycle in OdinSync, from user login to session termination.

Unlike the previous documents, which describe individual components, this document illustrates how they interact during runtime to provide secure authentication and session management.

Related Documents

* Part 11.2 – Login Session Creation
* Part 11.3 – Refresh Token Rotation
* Part 11.4 – Replay Detection
* Part 11.5 – Logout & Session Revocation
* Part 11.6 – Logout All Devices
* Part 11.7 – Transaction Management & Concurrency
* Part 11.8 – Exception Handling

⸻

Authentication Lifecycle

A typical authenticated session progresses through the following stages.

User Login
↓
Authentication
↓
Login Session Creation
↓
Protected API Access
↓
JWT Expiration
↓
Refresh Token Rotation
↓
Protected API Access
↓
Logout / Logout All
↓
Session Terminated

Each stage is independent but builds upon the previous one.

⸻

Login Flow

Client
↓
POST /api/v1/auth/login
↓
AuthenticationController
↓
AuthenticationService
↓
AuthenticationManager
↓
Password Verification
↓
LoginSessionService
↓
AccessTokenIssuer
↓
RefreshTokenService
↓
Persist Refresh Session
↓
Return Access Token
+
Return Refresh Token

Result:

* JWT access token issued.
* Refresh session created.
* Token family initialized.

⸻

Protected API Request

For every secured request:

Client
↓
Authorization: Bearer JWT
↓
Spring Security Filter Chain
↓
JWT Validation
↓
JwtAuthenticationConverter
↓
SecurityContext
↓
Controller
↓
Business Logic

If JWT validation succeeds:

* SecurityContext is populated.
* Authorization rules are evaluated.
* Request proceeds.

⸻

Expired JWT

When the access token expires:

Protected API
↓
JWT Expired
↓
401 Unauthorized

The client then initiates the refresh flow.

⸻

Refresh Flow

Client
↓
POST /api/v1/auth/refresh
↓
RefreshTokenApplicationService
↓
Hash Refresh Token
↓
Lookup Refresh Session
↓
Validate Session
↓
Generate JWT
↓
Generate Refresh Token
↓
Rotate Refresh Session
↓
Commit Transaction
↓
Return New Token Pair

Result:

* Previous refresh token becomes ROTATED.
* New refresh token becomes ACTIVE.
* New JWT access token is issued.

⸻

Replay Detection

If a rotated refresh token is reused:

Client
↓
POST /auth/refresh
↓
ROTATED Session
↓
Replay Detected
↓
Revoke Token Family
↓
401 Unauthorized
↓
User Must Login Again

No new tokens are issued.

⸻

Logout Flow

Client
↓
POST /api/v1/auth/logout
↓
Locate Refresh Session
↓
Mark Session REVOKED
↓
Commit
↓
204 No Content

The refresh token becomes unusable.

Previously issued JWTs remain valid until expiration.

⸻

Logout All Devices

Client
↓
POST /api/v1/auth/logout-all
↓
Find Active Sessions
↓
Revoke Sessions
↓
Commit
↓
204 No Content

All refresh sessions for the user are revoked.

⸻

Session State Transitions

ACTIVE
↓
ROTATED
↓
REVOKED
↓
EXPIRED

Only ACTIVE sessions may participate in refresh token rotation.

⸻

Database Lifecycle

During a normal login and refresh cycle:

Login
↓
Insert ACTIVE Session
↓
Refresh
↓
Update Existing → ROTATED
↓
Insert New ACTIVE Session
↓
Logout
↓
Update ACTIVE → REVOKED

Historical records are preserved.

⸻

SecurityContext Lifecycle

Incoming Request
↓
JWT Validation
↓
SecurityContext Created
↓
Controller Execution
↓
Response Returned
↓
SecurityContext Cleared

The SecurityContext exists only for the lifetime of the request.

⸻

Failure Scenarios

Stage	Result
Invalid credentials	401 Unauthorized
Invalid JWT	401 Unauthorized
Expired JWT	401 Unauthorized
Invalid refresh token	401 Unauthorized
Replay detected	401 Unauthorized
User disabled	403 Forbidden
Tenant disabled	403 Forbidden

Clients should redirect users to the login screen whenever authentication cannot be re-established.

⸻

Runtime Components

AuthenticationController
↓
AuthenticationService
↓
LoginSessionService
↓
RefreshTokenService
↓
RefreshTokenStore
↓
Database

Protected API authentication additionally involves:

SecurityFilterChain
↓
JwtDecoder
↓
JwtAuthenticationConverter
↓
SecurityContext

⸻

Monitoring

Recommended metrics:

* Successful logins
* Failed logins
* Refresh requests
* Successful rotations
* Replay detections
* Logout requests
* Logout-all requests
* Active session count
* Authentication latency

These metrics should be exported to the application’s monitoring platform.

⸻

Operational Considerations

For production deployments:

* Keep JWT access tokens short-lived.
* Rotate refresh tokens on every refresh.
* Use HTTPS exclusively.
* Publish audit events after transaction commit.
* Preserve revoked sessions for investigation.
* Periodically remove expired sessions using a scheduled cleanup job.

⸻

End-to-End Summary

Login
↓
JWT + Refresh Token
↓
Protected APIs
↓
JWT Expires
↓
Refresh
↓
New JWT + Refresh Token
↓
Protected APIs
↓
Logout
↓
Session Revoked

This represents the normal lifecycle of an authenticated session.

⸻

Design Principles

The OdinSync runtime authentication model is based on:

* Stateless JWT access tokens.
* Stateful refresh sessions.
* Single-use refresh tokens.
* Atomic token rotation.
* Transactional consistency.
* Independent device sessions.
* Replay detection.
* Complete audit history.
* Short-lived access tokens.
* Secure session revocation.

⸻

Summary

The runtime authentication flow brings together all components of OdinSync’s authentication subsystem into a cohesive session lifecycle. A successful login establishes an authenticated session backed by a refresh token, protected API requests rely on stateless JWT validation, refresh requests rotate session credentials atomically, replay detection protects against stolen tokens, and logout operations revoke refresh sessions while preserving audit history. This lifecycle provides a secure, scalable, and production-ready authentication model suitable for distributed deployments and multi-device user sessions.


Future enhancements

The only thing I’d keep is a short Future Enhancements section in your roadmap (not a large document), for items you’ve intentionally deferred:

* authorization_version for immediate permission revocation
* Redis-backed authorization version lookup
* MFA / Passkeys
* OAuth2 social login
* Device/session management UI
* Risk-based authentication