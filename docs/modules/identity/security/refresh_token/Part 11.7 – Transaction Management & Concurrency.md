Part 11.7 – Transaction Management & Concurrency

Project: OdinSync
Module: Identity & Access Management
Component: Refresh Token Service

⸻

Overview

This document defines how OdinSync guarantees atomic, consistent, and thread-safe refresh token operations.

Since refresh token rotation modifies persistent session state, every operation must be protected against concurrent requests, race conditions, and partial database updates.

This document focuses on transaction boundaries and concurrency control used during session management.

Related Documents

* Part 11.3 – Refresh Token Rotation
* Part 11.4 – Replay Detection
* Part 11.5 – Logout & Session Revocation
* Part 11.6 – Logout All Devices

⸻

Objectives

The implementation must guarantee:

* One successful refresh operation per refresh token.
* Atomic database updates.
* Consistent session state.
* No duplicate active refresh tokens.
* Safe concurrent request handling.
* Complete rollback on failure.

⸻

Transaction Boundaries

Every operation that changes refresh session state must execute inside a database transaction.

Operations requiring transactions:

* Login Session Creation
* Refresh Token Rotation
* Logout
* Logout All Devices
* Replay Detection / Family Revocation

Read-only operations should use read-only transactions where appropriate.

⸻

Refresh Token Rotation Transaction

The refresh operation should be treated as a single atomic unit.

Start Transaction
↓
Lock Refresh Session
↓
Validate Session
↓
Generate New Tokens
↓
Update Existing Token
↓
Insert Replacement Token
↓
Commit Transaction

If any step fails, the entire transaction is rolled back.

⸻

Why Atomic Rotation?

Without a transaction, failures can leave the database in an inconsistent state.

Example:

Generate New Refresh Token
✓
Update Old Token
✓
Insert New Token
✗ Failure

Result:

* Previous token is no longer usable.
* Replacement token does not exist.
* User session becomes unusable.

Executing the entire operation in one transaction prevents this scenario.

⸻

Concurrency Problem

Consider two refresh requests arriving almost simultaneously.

Request A
↓
Refresh Token X
──────────────
Request B
↓
Refresh Token X

Without locking:

* Both requests read the same ACTIVE session.
* Both generate replacement tokens.
* Two valid refresh tokens are issued.

This violates the single-use refresh token policy.

⸻

Locking Strategy

OdinSync uses pessimistic row-level locking during refresh token rotation.

Typical repository method:

@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

The first request acquires the database lock.

Subsequent requests wait until the transaction completes.

⸻

Concurrent Refresh Behaviour

Example:

Request A
Lock Session
↓
Validate
↓
Rotate
↓
Commit
──────────────
Request B
Wait
↓
Read Updated Session
↓
ROTATED
↓
Reject Request

Only one request succeeds.

⸻

Isolation Level

The default database isolation level is generally sufficient when combined with pessimistic locking.

Recommendations:

* Avoid lowering isolation for refresh operations.
* Keep transactions short.
* Minimize work performed while holding locks.

⸻

Rollback Behaviour

If an exception occurs before commit:

* Old refresh token remains unchanged.
* New refresh token is discarded.
* No partial updates are persisted.

Rollback ensures session consistency.

⸻

Logout Transaction

Logout follows a similar pattern.

Start Transaction
↓
Find Session
↓
Validate
↓
Mark REVOKED
↓
Commit

Either the session is revoked successfully or no change is applied.

⸻

Logout All Transaction

Global logout executes as a single transaction.

Load Active Sessions
↓
Mark All REVOKED
↓
Commit

If the transaction fails, none of the sessions are partially updated.

⸻

Replay Detection Transaction

Replay detection should revoke the token family atomically.

Replay Detected
↓
Load Family
↓
Mark Sessions REVOKED
↓
Commit

This prevents partial family revocation.

⸻

Deadlock Considerations

To reduce deadlock risk:

* Always lock resources in a consistent order.
* Keep transactions short.
* Avoid remote service calls inside transactions.
* Avoid unnecessary database queries while holding locks.

⸻

Operations Outside Transactions

The following operations should occur before opening a transaction whenever possible:

* Request validation
* Input parsing
* DTO mapping
* Basic format validation

The following operations should occur after a successful commit:

* Audit event publication
* Metrics updates
* Notification dispatch
* Asynchronous processing

This minimizes lock duration and improves throughput.

⸻

Idempotency

Logout operations should be idempotent.

Examples:

* Logging out an already revoked session should return success.
* Repeating “Logout All Devices” should not produce an error.

Refresh token rotation is not idempotent.

Reusing the same refresh token is treated as a replay attempt.

⸻

Clustered Deployments

The concurrency strategy must work correctly across multiple application instances.

Since session state is stored in the shared database:

* any application instance can process a refresh request
* database locks coordinate concurrent access
* no server affinity is required

This enables horizontal scaling without additional coordination.

⸻

Monitoring

Recommended operational metrics:

* Successful refresh operations
* Failed refresh operations
* Replay detections
* Lock wait duration
* Transaction rollback count
* Average refresh latency

These metrics help identify authentication issues and database contention.

⸻

Testing

Concurrency testing should verify:

* Two simultaneous refresh requests.
* Multiple concurrent logout requests.
* Rollback on persistence failure.
* Replay detection after rotation.
* Clustered deployment behaviour.
* Database lock contention.

⸻

Design Principles

The OdinSync implementation follows these principles:

* Atomic state changes.
* Database-backed consistency.
* Pessimistic locking for rotation.
* Short-lived transactions.
* Idempotent logout operations.
* No duplicate active refresh tokens.
* Cluster-safe session management.

⸻

Implementation Notes

During implementation:

* Keep business logic inside the service layer.
* Limit repository responsibilities to persistence.
* Do not perform external API calls while holding database locks.
* Publish domain events only after a successful transaction commit.
* Ensure repository queries support row-level locking where required.

⸻

Summary

Transaction management is the foundation of OdinSync’s refresh token implementation. By combining transactional updates with pessimistic row-level locking, the system guarantees that refresh token rotation, replay detection, and session revocation remain consistent even under concurrent requests and across multiple application instances. This approach preserves the single-use refresh token model while providing predictable behaviour in production environments.