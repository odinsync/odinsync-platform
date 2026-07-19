# Refresh Token Rotation
## Part 9 – Repository Design & Database Locking

> **Project:** OdinSync  
> **Module:** Identity & Access Management  
> **Document:** Refresh Token Rotation  
> **Part:** 9 – Repository Design & Database Locking

---

# Overview

After designing the `RefreshTokenJpaEntity`, the next step is creating the repository layer.

The repository is responsible for:

- Retrieving refresh tokens
- Persisting new refresh tokens
- Updating token status
- Finding token families
- Applying database locks during token rotation

Unlike a typical CRUD repository, the refresh token repository plays an important role in maintaining the security and consistency of the authentication system.

---

# Responsibilities

The `RefreshTokenRepository` should:

- Find a refresh token by its hash.
- Lock a refresh token during rotation.
- Save newly generated refresh tokens.
- Retrieve all tokens belonging to a token family.
- Revoke an entire token family.
- Delete expired tokens (cleanup job).

It should **not**:

- Generate tokens.
- Hash tokens.
- Validate JWTs.
- Authenticate users.
- Perform business logic.

Those responsibilities belong to the service layer.

---

# Repository Structure

```text
RefreshTokenService
        │
        ▼
RefreshTokenRepository
        │
        ▼
RefreshTokenJpaEntity
        │
        ▼
Database
```

The repository acts as the persistence layer between the application and the database.

---

# Repository Interface

```java
@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshTokenJpaEntity, UUID> {

}
```

Spring Data JPA automatically provides common operations:

- save()
- findById()
- delete()
- existsById()

Custom queries are added for refresh token operations.

---

# Find by Token Hash

Since only the SHA-256 hash is stored, every refresh request first hashes the incoming token and searches using the hash.

```java
Optional<RefreshTokenJpaEntity>
findByTokenHash(String tokenHash);
```

Flow:

```
Client Token

↓

SHA-256

↓

Repository

↓

Database Lookup
```

The raw refresh token is never used in queries.

---

# Why Search by Hash?

Suppose the incoming refresh token is:

```
Yn3PqA4hJ...
```

The server computes:

```
SHA-256

↓

4fd18ab3...
```

Only this hash is compared with the stored value.

```
Incoming Token

↓

Hash

↓

Database Match
```

Even if the database is compromised, the original refresh token cannot be retrieved.

---

# Why Database Locking?

Consider two refresh requests arriving almost simultaneously.

```
Request A

↓

Refresh R1
```

```
Request B

↓

Refresh R1
```

Without locking:

1. Both requests read R1.
2. Both believe R1 is active.
3. Both generate new refresh tokens.
4. Two valid refresh tokens now exist.

```
R1

├── R2

└── R3
```

This violates the single-use refresh token rule.

---

# Pessimistic Locking

To prevent concurrent updates, OdinSync locks the refresh token row while processing a refresh request.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

Flow:

```
Request A

↓

Acquire Lock

↓

Rotate Token

↓

Commit

↓

Release Lock
```

Meanwhile:

```
Request B

↓

Wait

↓

Read Updated State

↓

Reject
```

Only one request can rotate the token.

---

# Repository Method with Lock

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
SELECT rt
FROM RefreshTokenJpaEntity rt
WHERE rt.tokenHash = :tokenHash
""")
Optional<RefreshTokenJpaEntity>
findByTokenHashForUpdate(String tokenHash);
```

This ensures that the selected row is locked until the transaction completes.

---

# Why PESSIMISTIC_WRITE?

`PESSIMISTIC_WRITE` prevents other transactions from modifying the same row until the current transaction finishes.

```
Transaction A

↓

Lock Row

↓

Update

↓

Commit
```

```
Transaction B

↓

Wait

↓

Continue After Commit
```

This guarantees that only one refresh request succeeds.

---

# Why Not Optimistic Locking?

Optimistic locking is useful when update conflicts are rare.

```
Version = 1

↓

Read

↓

Update

↓

Version = 2
```

However, refresh token rotation is a security-sensitive operation.

Concurrent refresh requests can occur because of:

- Browser retries
- Mobile retries
- Network latency
- Multiple browser tabs

Using pessimistic locking ensures correctness instead of relying on retry logic.

---

# Finding All Tokens in a Family

When replay is detected, OdinSync revokes every token in the same family.

Repository method:

```java
List<RefreshTokenJpaEntity>
findByFamilyId(UUID familyId);
```

Example:

```
Family F100

↓

R1

R2

R3
```

This query retrieves every token belonging to the session.

---

# Revoking a Token Family

During replay detection:

```
Replay

↓

Find Family

↓

Revoke Every Token
```

Example repository method:

```java
List<RefreshTokenJpaEntity>
findAllByFamilyId(UUID familyId);
```

The service updates each entity inside the same transaction.

---

# Cleanup Queries

Expired refresh tokens should not remain forever.

Typical cleanup query:

```java
List<RefreshTokenJpaEntity>
findAllByExpiresAtBefore(Instant now);
```

A scheduled cleanup job can periodically remove expired records.

---

# Repository Workflow

```
Refresh Request

↓

Hash Token

↓

Find Token With Lock

↓

Validate

↓

Generate New Token

↓

Save

↓

Commit
```

Every refresh operation follows this sequence.

---

# Design Decisions

The repository layer follows these principles:

- Search using token hashes.
- Never expose raw refresh tokens.
- Use pessimistic locking for rotation.
- Keep business logic in services.
- Support efficient replay detection.
- Enable family-based session management.

---

# Looking Ahead

The repository provides secure database access, but it does not implement refresh token rotation.

The next chapter introduces the **RefreshTokenService**, where we will implement:

- Refresh token generation
- Token hashing
- Session creation
- Token rotation
- Replay detection
- Family revocation
- Transaction management

This service becomes the core of OdinSync's refresh token implementation.

---

# Summary

The `RefreshTokenRepository` is responsible for securely accessing refresh token data while preserving session consistency.

Key capabilities include:

- Looking up refresh tokens using SHA-256 hashes.
- Locking refresh token rows during rotation.
- Retrieving all tokens in a session family.
- Supporting replay detection.
- Enabling cleanup of expired sessions.

By combining secure lookups with `PESSIMISTIC_WRITE` locking, OdinSync guarantees that a refresh token can only be successfully rotated once, even under concurrent requests.