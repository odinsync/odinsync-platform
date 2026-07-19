Refresh Token Rotation

Part 10 – Secure Token Generation, Hashing & Configuration

Project: OdinSync
Module: Identity & Access Management
Document: Refresh Token Rotation
Part: 10 – Secure Token Generation, Hashing & Configuration

⸻

Overview

The previous chapters established how refresh tokens are stored, retrieved, and protected from concurrent access.

This chapter focuses on how refresh tokens are generated, hashed, and configured.

A refresh token is effectively a long-lived authentication credential. Unlike a JWT, it is an opaque random value with no embedded claims or business information.

Its security depends entirely on:

* Cryptographically secure random generation.
* Sufficient entropy.
* Secure hashing before persistence.
* Proper expiration.
* Secure comparison during validation.

⸻

Objectives

The refresh token implementation should:

* Generate cryptographically secure random tokens.
* Produce tokens that are practically impossible to guess.
* Store only token hashes.
* Never log raw refresh tokens.
* Allow expiration to be configured.
* Keep generation logic separate from business logic.

⸻

Refresh Token Lifecycle

User Login
      │
      ▼
Generate Random Token
      │
      ▼
Return Raw Token to Client
      │
      ▼
Hash Token (SHA-256)
      │
      ▼
Store Hash in Database

Later:

Incoming Refresh Token
      │
      ▼
Hash Token
      │
      ▼
Lookup Database
      │
      ▼
Validate Session

The database never stores the original refresh token.

⸻

Why Refresh Tokens Are Opaque

Unlike JWT access tokens, refresh tokens contain no claims.

A JWT contains information such as:

* Subject
* Roles
* Tenant
* Expiration
* Issued time

A refresh token contains nothing except secure random data.

Example:

YL7nqN5VwRDbR2pM0Lxv4JjAcQm8kTpZ9fHs1KdN...

The server understands the token only after hashing it and locating the associated session.

⸻

Why Not Use JWT as a Refresh Token?

Although technically possible, OdinSync deliberately uses opaque refresh tokens.

Advantages include:

* Immediate revocation.
* Replay detection.
* Session tracking.
* Device management.
* Simpler rotation.
* No sensitive claims exposed.

JWT refresh tokens are harder to revoke because they remain self-contained after issuance.

⸻

Token Generator Responsibilities

The token generator has one responsibility:

Produce a cryptographically secure random refresh token.

It should not:

* Hash tokens.
* Persist tokens.
* Authenticate users.
* Generate JWTs.

Keeping responsibilities separate improves maintainability and testing.

⸻

Secure Random Generation

Refresh tokens must be generated using a cryptographically secure random number generator.

Example:

SecureRandom secureRandom = new SecureRandom();

Do not use:

* java.util.Random
* Predictable counters
* UUIDs alone
* Timestamps

These are not designed to generate authentication secrets.

⸻

Recommended Token Length

A refresh token should contain sufficient entropy to resist brute-force attacks.

Recommended characteristics:

Property	Recommendation
Random Source	SecureRandom
Entropy	≥ 256 bits
Encoding	Base64 URL-safe
Lifetime	Configurable (e.g., 30 days)

The exact encoded string length depends on the chosen encoding.

⸻

Base64 URL Encoding

Refresh tokens are typically encoded using URL-safe Base64.

Advantages:

* Safe for HTTP transport.
* No whitespace.
* No binary data.
* Easy to store and transmit.

Example:

X3sYqM8xN7L4PzBkRwJ9hFaQ...

⸻

Token Generator

Example implementation:

@Component
public class SecureRefreshTokenGenerator {
    private static final int TOKEN_SIZE = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    public String generate() {
        byte[] bytes = new byte[TOKEN_SIZE];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}

Responsibilities:

* Produce random bytes.
* Encode using Base64 URL-safe encoding.
* Return the raw refresh token.

⸻

Hashing Refresh Tokens

Before persistence, the token is hashed.

Raw Token
      │
      ▼
SHA-256
      │
      ▼
Database

The raw value is discarded after the response is sent to the client.

⸻

Why Hash Refresh Tokens?

Suppose an attacker steals the database.

Without hashing:

Database Leak
      │
      ▼
Attacker Uses Token

With hashing:

Database Leak
      │
      ▼
Attacker Gets Hash
      │
      ▼
Cannot Authenticate

Hashing significantly reduces the impact of database compromise.

⸻

Refresh Token Hasher

Example:

@Component
public class RefreshTokenHasher {
    public String hash(String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }
}

The same input always produces the same hash, allowing efficient lookups.

⸻

Validation Flow

When a client submits a refresh token:

Incoming Token
      │
      ▼
Hash
      │
      ▼
Find Database Record
      │
      ▼
Validate Session

The raw token itself is never compared directly with database values.

⸻

Configuration Properties

Refresh-token behavior should be externally configurable.

Example:

security:
  refresh-token:
    expiration: 30d
    token-length: 32

This avoids recompiling the application when changing security policies.

⸻

Configuration Class

@ConfigurationProperties(prefix = "security.refresh-token")
public class RefreshTokenProperties {
    private Duration expiration;
    private int tokenLength;
    // getters & setters
}

The generator and service should depend on this configuration rather than hardcoded values.

⸻

Issued Refresh Token

The service often needs more than just the raw token.

Example record:

public record IssuedRefreshToken(
        String rawToken,
        String tokenHash,
        Instant expiresAt
) {
}

This keeps related values together and reduces parameter passing.

⸻

Separation of Responsibilities

SecureRefreshTokenGenerator
        │
        └── Generates raw token
RefreshTokenHasher
        │
        └── Hashes token
RefreshTokenService
        │
        ├── Creates session
        ├── Stores hash
        └── Returns raw token

Each class has a single, well-defined responsibility.

⸻

Logging Considerations

Never log:

* Raw refresh tokens.
* Authorization headers.
* Cookie values.
* Token hashes in debug logs.

Safe logging includes:

* User ID.
* Tenant ID.
* Family ID.
* Refresh token ID.
* Timestamp.

Logs should assist investigations without exposing credentials.

⸻

Testing

Important unit tests include:

* Generator returns non-empty tokens.
* Consecutive tokens are unique.
* Hashing is deterministic.
* Different tokens produce different hashes.
* Generated expiration matches configured duration.
* Token length follows configuration.

Security-sensitive components should have high test coverage.

⸻

Design Decisions

OdinSync adopts the following approach:

* Opaque refresh tokens.
* SecureRandom generation.
* Base64 URL-safe encoding.
* SHA-256 hash persistence.
* Configurable expiration.
* Separate generator and hasher classes.
* No raw token persistence.
* No credential logging.

⸻

Looking Ahead

The building blocks are now complete:

* Database schema
* JPA entity
* Repository
* Token generation
* Token hashing

The next chapter combines these components into the core business service:

* RefreshTokenService
* Login session creation
* Refresh token rotation
* Replay detection
* Family revocation
* Transaction management

This service becomes the central coordinator of refresh token authentication.

⸻

Summary

Refresh tokens are long-lived authentication credentials that require stronger handling than ordinary application data.

OdinSync secures them by:

* Generating high-entropy random values.
* Using SecureRandom.
* Encoding with URL-safe Base64.
* Persisting only SHA-256 hashes.
* Making expiration configurable.
* Separating generation, hashing, and business logic.

These components provide the cryptographic foundation for secure refresh token rotation.