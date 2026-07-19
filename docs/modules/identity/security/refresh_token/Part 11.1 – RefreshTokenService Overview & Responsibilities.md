Refresh Token Rotation

Part 11.1 – RefreshTokenService Overview & Responsibilities

Project: OdinSync
Module: Identity & Access Management
Document: Refresh Token Rotation
Part: 11.1 – RefreshTokenService Overview & Responsibilities

⸻

Overview

The previous chapters introduced every building block required for refresh token authentication:

* Refresh token database schema
* JPA entity
* Repository
* Token generator
* Token hasher
* Configuration

Each component has a single responsibility.

However, none of these components implements the refresh token workflow.

That responsibility belongs to RefreshTokenService.

This service orchestrates the complete lifecycle of a refresh token, from initial session creation to rotation, replay detection, and session termination.

⸻

Position in the Authentication Architecture

                Login API
                    │
                    ▼
        AuthenticationService
                    │
      Password Verification
                    │
                    ▼
        JwtTokenGenerator
                    │
                    ▼
      RefreshTokenService
                    │
      ┌─────────────┴──────────────┐
      ▼                            ▼
Generate Token             Persist Session
      │                            │
      └─────────────┬──────────────┘
                    ▼
          Login Response

For refresh operations:

Client
    │
    ▼
/auth/refresh
    │
    ▼
RefreshTokenService
    │
    ▼
Repository
    │
    ▼
Database

Every refresh request passes through this service.

⸻

Why a Dedicated Service?

Without a dedicated service, refresh logic becomes scattered across multiple classes.

Example:

Controller
↓
Repository
↓
JWT Generator
↓
Hasher
↓
Database
↓
Replay Detection
↓
Logout Logic

This results in:

* Duplicated code.
* Inconsistent validation.
* Difficult testing.
* Increased security risk.

Instead:

Controller
↓
RefreshTokenService
↓
Other Components

Controllers remain thin while business rules stay centralized.

⸻

Core Responsibilities

RefreshTokenService is responsible for:

* Creating refresh-token sessions.
* Rotating refresh tokens.
* Validating session state.
* Detecting replay attacks.
* Revoking sessions.
* Revoking token families.
* Managing logout.
* Managing logout-all-devices.
* Coordinating transactions.

It is not responsible for:

* Password verification.
* Loading users.
* Creating Authentication objects.
* Validating JWT access tokens.
* Authorizing API requests.

Those responsibilities belong elsewhere in the authentication system.

⸻

Single Responsibility Principle

RefreshTokenService should coordinate existing components rather than implementing cryptography itself.

RefreshTokenService
├── Generator
├── Hasher
├── Repository
├── JWT Generator
├── Clock
└── Configuration

The service orchestrates them.

⸻

Dependencies

A typical constructor might look like:

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final SecureRefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHasher tokenHasher;
    private final RefreshTokenRepository repository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final Clock clock;
    private final RefreshTokenProperties properties;
}

Every dependency has a well-defined purpose.

⸻

Dependency Responsibilities

SecureRefreshTokenGenerator

Responsible for:

* Creating random refresh tokens.

Never:

* Saves tokens.
* Hashes tokens.
* Reads the database.

⸻

RefreshTokenHasher

Responsible for:

Raw Token
↓
SHA-256
↓
Hash

No persistence logic.

⸻

RefreshTokenRepository

Responsible for:

* Loading sessions.
* Locking rows.
* Saving entities.
* Revoking sessions.

No business decisions.

⸻

JwtTokenGenerator

Responsible for issuing:

* Access tokens.

RefreshTokenService never builds JWTs directly.

⸻

Clock

Instead of:

Instant.now();

OdinSync uses:

clock.instant();

Benefits:

* Deterministic tests.
* Simulated expiration.
* Consistent time source.

⸻

RefreshTokenProperties

Provides:

* Token lifetime.
* Token length.
* Future refresh-token settings.

Configuration stays outside the service implementation.

⸻

High-Level Responsibilities

RefreshTokenService exposes four major operations.

createSession()
rotate()
logout()
logoutAll()

Each operation has its own workflow.

⸻

Operation 1 — Create Session

Called after successful login.

Authenticate User
↓
Generate JWT
↓
Generate Refresh Token
↓
Hash Refresh Token
↓
Persist Session
↓
Return Token Pair

This creates the initial authenticated session.

⸻

Operation 2 — Rotate Session

Triggered by:

POST /auth/refresh

Workflow:

Receive Refresh Token
↓
Hash
↓
Find Session
↓
Validate
↓
Generate New JWT
↓
Generate New Refresh Token
↓
Revoke Current Token
↓
Save New Session
↓
Return New Pair

This is the most complex service operation.

⸻

Operation 3 — Logout

Triggered by:

POST /auth/logout

Workflow:

Receive Refresh Token
↓
Hash
↓
Find Session
↓
Revoke Session
↓
Success

No replacement token is created.

⸻

Operation 4 — Logout All Devices

Workflow:

User
↓
Find Active Sessions
↓
Revoke Every Session
↓
Logout Everywhere

Every active refresh token becomes invalid.

⸻

Service Workflow

Refresh Request
↓
Hash Token
↓
Repository
↓
Validate
↓
Business Rules
↓
Generate New Tokens
↓
Persist
↓
Return Response

Every request follows this pipeline.

⸻

Why Business Logic Belongs Here

The service decides:

* Is the token expired?
* Is it revoked?
* Was it reused?
* Should the family be revoked?
* Should a new JWT be issued?

Repositories simply execute persistence operations.

Generators simply generate values.

This separation improves maintainability.

⸻

Service Does Not Know HTTP

RefreshTokenService should not reference:

* HttpServletRequest
* HttpServletResponse
* Cookies
* REST controllers

Instead:

Controller
↓
DTO
↓
Service
↓
DTO

This keeps the service reusable across REST, GraphQL, messaging, or future APIs.

⸻

Transaction Boundary

Every refresh operation should execute inside one database transaction.

@Transactional
public RefreshResponse rotate(...) {
}

The transaction guarantees:

* Atomic updates.
* Consistent rotation.
* Proper locking.
* Reliable replay detection.

Transaction details are covered later in this chapter.

⸻

Expected Public API

A clean service API might resemble:

public interface RefreshTokenService {
    LoginSession createSession(
            AuthenticatedUser user);
    RefreshResponse rotate(
            String refreshToken);
    void logout(
            String refreshToken);
    void logoutAll(
            UUID userId,
            UUID tenantId);
}

The interface describes business operations rather than persistence details.

⸻

Typical Class Diagram

                    RefreshTokenService
                             │
     ┌──────────────┬─────────┴──────────────┬───────────────┐
     ▼              ▼                        ▼               ▼
Generator       Hasher                 Repository      JwtGenerator
     │              │                        │               │
     └──────────────┴──────────────┬─────────┴───────────────┘
                                   ▼
                              Business Flow

The service coordinates all supporting components.

⸻

Error Responsibilities

RefreshTokenService determines business failures such as:

* Refresh token not found.
* Token expired.
* Token revoked.
* Replay detected.
* Session terminated.

It throws domain-specific exceptions.

Controllers translate those exceptions into HTTP responses.

⸻

Security Principles

The service follows several key principles:

* Never trust client-provided state.
* Always validate the latest database state.
* Never issue multiple active successors.
* Never reuse refresh tokens.
* Never expose persistence details.
* Keep transactions short.
* Keep cryptography isolated.

⸻

Design Decisions

OdinSync’s RefreshTokenService:

* Coordinates rather than implements cryptography.
* Depends on abstractions.
* Owns business rules.
* Defines transaction boundaries.
* Centralizes refresh token workflows.
* Remains independent of HTTP and persistence details.

⸻

Looking Ahead

The next section implements the first business workflow:

Part 11.2 – Login Session Creation

Topics include:

* Creating a token family.
* Generating refresh tokens.
* Persisting the initial session.
* Returning the login response.
* Integration with AuthenticationService.
* Sequence diagrams.
* Complete implementation walkthrough.

⸻

Summary

RefreshTokenService is the central coordinator of OdinSync’s refresh token architecture.

It brings together the generator, hasher, repository, JWT generator, and configuration to implement the complete refresh token lifecycle while maintaining clear separation of responsibilities.

All refresh token business rules—including session creation, rotation, replay detection, and logout—are centralized here, making the authentication system easier to test, maintain, and evolve.