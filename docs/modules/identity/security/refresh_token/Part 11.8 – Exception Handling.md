Part 11.8 – Exception Handling

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document defines the exception handling strategy for the Refresh Token Service.

The objective is to provide:

* Consistent API responses
* Secure error handling
* Centralized exception mapping
* Clear audit logging
* Minimal information disclosure

Authentication endpoints should never expose internal implementation details or reveal why a refresh request failed beyond what is necessary for the client to recover.

Related Documents

* Part 11.3 – Refresh Token Rotation
* Part 11.4 – Replay Detection
* Part 11.7 – Transaction Management & Concurrency

⸻

Exception Handling Principles

The implementation follows these principles:

* Business services throw domain-specific exceptions.
* Controllers do not contain authentication error handling logic.
* Global exception handling produces consistent HTTP responses.
* Internal exception details are never exposed to clients.
* All security-related failures are logged for auditing.

⸻

Exception Hierarchy

A dedicated hierarchy keeps authentication errors organized.

AuthenticationException
│
├── InvalidRefreshTokenException
├── RefreshTokenExpiredException
├── RefreshTokenRevokedException
├── RefreshTokenReplayDetectedException
├── SessionNotFoundException
├── SessionLimitExceededException
├── UserAccountDisabledException
└── TenantDisabledException

Additional exceptions may be introduced as the authentication module evolves.

⸻

Exception Mapping

Exception	HTTP Status
InvalidRefreshTokenException	401 Unauthorized
RefreshTokenExpiredException	401 Unauthorized
RefreshTokenRevokedException	401 Unauthorized
RefreshTokenReplayDetectedException	401 Unauthorized
SessionNotFoundException	401 Unauthorized
UserAccountDisabledException	403 Forbidden
TenantDisabledException	403 Forbidden
SessionLimitExceededException	409 Conflict
IllegalArgumentException	400 Bad Request
Unexpected Exception	500 Internal Server Error

⸻

Error Response

All authentication endpoints should return a consistent error structure.

Example:

{
  "timestamp": "2026-07-19T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "AUTH-401",
  "message": "Authentication required.",
  "path": "/api/v1/auth/refresh",
  "requestId": "e9e93b40..."
}

Recommended fields:

* timestamp
* status
* error
* code
* message
* path
* requestId

The response should be stable across all authentication APIs.

⸻

Information Disclosure

Authentication failures should not reveal internal validation results.

Do not return messages such as:

* Refresh token expired
* Refresh token revoked
* User does not exist
* Token family revoked

Instead, return a generic authentication failure message.

Detailed information belongs only in server logs.

⸻

Global Exception Handler

Authentication exceptions should be translated into HTTP responses by a centralized exception handler.

Responsibilities:

* Map domain exceptions to HTTP status codes.
* Build standardized error responses.
* Record request identifiers.
* Publish audit events when required.

Business services remain independent of HTTP concerns.

⸻

Logging Guidelines

Security-related exceptions should generate structured logs.

Recommended fields:

* Request ID
* User ID (if known)
* Tenant ID (if known)
* Session ID (if available)
* Token Family ID (if available)
* Client IP
* User Agent
* Exception Type
* Timestamp

Never log:

* JWT access tokens
* Refresh tokens
* Refresh token hashes
* Passwords
* Authorization headers

⸻

Audit Events

The following events should be recorded for operational monitoring:

* Invalid refresh token
* Replay detection
* Session revocation
* Logout
* Logout all devices
* User account disabled
* Tenant access denied

Audit events should be published only after a successful transaction commit.

⸻

Client Behaviour

Clients should interpret authentication failures consistently.

Recommended behaviour:

HTTP Status	Client Action
400	Correct request and retry
401	Redirect user to login
403	Display access denied message
409	Inform user of session conflict
500	Retry later or display generic error

Native and browser clients should follow the same authentication flow.

⸻

Testing

Exception handling should verify:

* Correct exception-to-status mapping.
* Standardized error response format.
* Generic client-facing messages.
* Sensitive information is never exposed.
* Structured logging is generated.
* Unexpected exceptions return HTTP 500.

⸻

Design Principles

The OdinSync implementation follows these principles:

* Centralized exception handling.
* Consistent API responses.
* Secure information disclosure.
* Structured operational logging.
* Separation of business logic and HTTP concerns.
* Stable client contracts.

⸻

Summary

A consistent exception handling strategy improves security, maintainability, and client integration. By using domain-specific exceptions, centralized HTTP mapping, and standardized error responses, OdinSync ensures predictable authentication behaviour while preventing unnecessary disclosure of internal security details. Structured logging and audit events provide operational visibility without exposing sensitive credentials.