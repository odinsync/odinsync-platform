# Refresh Token Rotation
## Part 8 – JPA Entity Design

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 8 – JPA Entity Design

---

# Overview

In the previous chapter, we designed the `refresh_tokens` database table.

This chapter maps that schema into a Spring Boot JPA entity.

The entity serves as the domain representation of an authenticated session and provides helper methods for common security checks such as:

- Is the token expired?
- Is the token revoked?
- Is the token active?
- Has the token been replaced?

Rather than scattering this logic throughout service classes, OdinSync keeps session-related behavior close to the entity.

---

# Entity Responsibilities

The `RefreshTokenJpaEntity` represents a single refresh token stored in the database.

Its responsibilities include:

- Mapping the database table.
- Representing refresh token metadata.
- Tracking token rotation.
- Supporting replay detection.
- Providing helper methods for validation.
- Maintaining audit information.

It is **not** responsible for:

- Generating refresh tokens.
- Hashing refresh tokens.
- Authenticating users.
- Generating JWTs.

Those responsibilities belong to dedicated services.

---

# Entity Structure

```text
RefreshTokenJpaEntity
│
├── id
├── userId
├── tenantId
├── tokenHash
├── familyId
├── replacedByTokenId
├── issuedAt
├── expiresAt
├── revokedAt
├── createdAt
└── updatedAt
```

Each instance represents one refresh token.

---

# Suggested Entity

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId;

    private UUID replacedByTokenId;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

This entity closely mirrors the database schema.

---

# Why UUID?

OdinSync uses UUIDs for identifiers.

Advantages include:

- Globally unique.
- No central ID generator.
- Safe in distributed systems.
- Hard to guess.
- Consistent across services.

Example:

```
550e8400-e29b-41d4-a716-446655440000
```

---

# Why Store tokenHash Instead of Token?

Only the SHA-256 hash of the refresh token is stored.

```
Raw Token

↓

SHA-256

↓

Database
```

Benefits:

- Protects against database leaks.
- Prevents token disclosure.
- Follows password storage best practices.

---

# Entity Relationships

The refresh token belongs to:

- One user.
- One tenant.
- One token family.

```
Tenant

↓

User

↓

Refresh Token
```

A user may have multiple refresh token families (one per device).

---

# Helper Methods

Instead of repeatedly writing validation logic inside services, helper methods improve readability.

---

## isExpired()

```java
public boolean isExpired(Clock clock) {
    return expiresAt.isBefore(clock.instant());
}
```

Purpose:

```
Current Time

↓

Compare

↓

Expired?
```

---

## isRevoked()

```java
public boolean isRevoked() {
    return revokedAt != null;
}
```

Purpose:

```
revokedAt == NULL

↓

Active
```

```
revokedAt != NULL

↓

Revoked
```

---

## isActive()

```java
public boolean isActive(Clock clock) {
    return !isExpired(clock) && !isRevoked();
}
```

This becomes the primary validation method.

Service classes can simply write:

```java
if (!token.isActive(clock)) {
    ...
}
```

instead of repeatedly checking multiple conditions.

---

## revoke()

```java
public void revoke(Instant revokedAt) {
    this.revokedAt = revokedAt;
}
```

Encapsulating this behavior keeps the service layer simple.

---

## replaceWith()

```java
public void replaceWith(UUID replacementTokenId,
                        Instant revokedAt) {

    this.replacedByTokenId = replacementTokenId;
    this.revokedAt = revokedAt;
}
```

This helper records both:

- which token replaced the current token,
- when the replacement occurred.

---

# Why Encapsulate Logic?

Compare:

### Without Helper Methods

```java
if (token.getRevokedAt() != null ||
    token.getExpiresAt().isBefore(clock.instant())) {
    ...
}
```

### With Helper Methods

```java
if (!token.isActive(clock)) {
    ...
}
```

The second version is:

- easier to read,
- easier to test,
- easier to maintain.

---

# Why Inject Clock?

Notice that helper methods receive a `Clock`.

Instead of:

```java
Instant.now()
```

OdinSync uses:

```java
Clock
```

Benefits:

- Deterministic unit tests.
- Easy expiry simulation.
- No dependence on system time.

Example:

```java
Clock fixedClock =
        Clock.fixed(...);
```

The service can now test expiration without waiting.

---

# Auditing

Spring Data JPA can automatically populate audit fields.

Configuration:

```java
@EnableJpaAuditing
```

Result:

```
Insert

↓

createdAt

↓

Automatically Populated
```

```
Update

↓

updatedAt

↓

Automatically Updated
```

This removes boilerplate code.

---

# Design Decisions

The entity follows several principles:

- Represents a single refresh token.
- Keeps validation logic close to the data.
- Stores hashes instead of raw tokens.
- Uses immutable identifiers.
- Supports auditing.
- Avoids business logic unrelated to persistence.

---

# Looking Ahead

The entity alone cannot retrieve refresh tokens.

The next chapter introduces the repository layer, including:

- `RefreshTokenRepository`
- Query methods
- `PESSIMISTIC_WRITE`
- Database locking
- Concurrent refresh handling

The repository will become the bridge between the entity and the refresh token service.

---

# Summary

The `RefreshTokenJpaEntity` models one refresh token and its associated session metadata.

It provides:

- Database mapping.
- Audit support.
- Helper validation methods.
- Rotation metadata.
- Replay detection metadata.

By keeping validation logic inside the entity and persistence concerns separate from authentication logic, OdinSync maintains a clean and maintainable architecture.