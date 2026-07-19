# Refresh Token Rotation
## Part 11.2 – Login Session Creation

> **Project:** OdinSync  
> **Module:** Identity & Access Management  
> **Document:** Refresh Token Rotation  
> **Part:** 11.2 – Login Session Creation

---

# Overview

The first responsibility of `RefreshTokenService` is to create a refresh-token session after a user successfully authenticates with an email and password.

This operation establishes the initial session state for the client.

The login flow must:

- Verify the user's credentials.
- Load the latest user and tenant context.
- Generate a signed access token.
- Create a new refresh-token family.
- Generate a cryptographically secure refresh token.
- Store only the refresh-token hash.
- Persist the initial session record.
- Return the raw refresh token to the client.
- Complete all state changes atomically.

The login operation creates the root token of a new token family.

```text
Login
  │
  ▼
Create family F100
  │
  ▼
Issue token R1
```

Every later token generated through refresh rotation remains part of the same family.

---

# Position in the Login Flow

The refresh-token session is created only after credential authentication succeeds.

```text
Client
  │
  ▼
POST /auth/login
  │
  ▼
LoginController
  │
  ▼
AuthenticationService
  │
  ├── AuthenticationManager
  ├── UserDetailsService
  └── PasswordEncoder
  │
  ▼
AuthenticatedUser
  │
  ▼
LoginSessionService
  │
  ├── AccessTokenIssuer
  └── RefreshTokenService
  │
  ▼
Token Pair
```

The refresh-token service must never validate passwords itself.

Password authentication remains the responsibility of the existing authentication flow.

---

# Separation Between Authentication and Session Creation

The login process has two distinct phases.

## Phase 1 — Authenticate Credentials

```text
Email + Password
      │
      ▼
AuthenticationManager
      │
      ▼
AuthenticatedUser
```

This phase answers:

> Are these credentials valid?

## Phase 2 — Create Authenticated Session

```text
AuthenticatedUser
      │
      ▼
Generate Access Token
      │
      ▼
Create Refresh Session
      │
      ▼
Return Token Pair
```

This phase answers:

> What authenticated session should be issued to this user?

These concerns should remain separate.

---

# Recommended Service Boundary

A dedicated login application service can coordinate both token types.

```java
public interface LoginSessionService {

    LoginTokenPair createSession(
            AuthenticatedUser authenticatedUser,
            ClientSessionContext clientContext);
}
```

Possible implementation:

```java
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl
        implements LoginSessionService {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public LoginTokenPair createSession(
            AuthenticatedUser authenticatedUser,
            ClientSessionContext clientContext) {

        IssuedAccessToken accessToken =
                accessTokenIssuer.issue(authenticatedUser);

        IssuedRefreshToken refreshToken =
                refreshTokenService.createSession(
                        authenticatedUser,
                        clientContext);

        return new LoginTokenPair(
                accessToken.tokenValue(),
                accessToken.expiresAt(),
                refreshToken.rawToken(),
                refreshToken.refreshToken().expiresAt());
    }
}
```

This design prevents the refresh-token service from becoming responsible for the entire login workflow.

---

# Why Use a Dedicated Login Session Service?

Without a coordinator, responsibilities may become mixed:

```text
AuthenticationService
  ├── Verifies password
  ├── Generates JWT
  ├── Generates refresh token
  ├── Saves session
  └── Builds HTTP response
```

That creates excessive coupling.

A better design is:

```text
AuthenticationService
  └── Verifies credentials

LoginSessionService
  └── Coordinates token issuance

AccessTokenIssuer
  └── Creates JWT

RefreshTokenService
  └── Creates refresh session
```

Each component remains focused.

---

# Create Session Operation

The refresh-token service may expose:

```java
IssuedRefreshToken createSession(
        AuthenticatedUser authenticatedUser,
        ClientSessionContext clientContext);
```

The operation creates one refresh-token record representing a new login session.

---

# Input Data

The operation requires trusted authentication data.

Example domain input:

```java
public record AuthenticatedUser(
        UUID userId,
        UUID tenantId,
        String email,
        Set<String> roles,
        Set<String> permissions,
        long authorizationVersion
) {
}
```

The service should receive this object from the completed authentication process.

It must not trust user ID, tenant ID, roles, or permissions supplied directly by the client.

---

# Client Session Context

Session metadata may be captured separately.

```java
public record ClientSessionContext(
        String deviceName,
        String userAgent,
        String ipAddress
) {
}
```

Possible fields include:

- Device name
- Browser or application user agent
- IP address
- Client type
- Application version
- Operating system

This metadata supports:

- Device management
- Session audit
- Suspicious login detection
- Logout from a specific device

The application should treat client-provided device labels as untrusted descriptive data.

---

# High-Level Creation Workflow

```text
AuthenticatedUser
      │
      ▼
Validate session eligibility
      │
      ▼
Create family ID
      │
      ▼
Generate raw refresh token
      │
      ▼
Hash raw token
      │
      ▼
Build refresh-token domain model
      │
      ▼
Persist record
      │
      ▼
Return raw token and metadata
```

---

# Step 1 — Validate Session Eligibility

Before creating a session, the service should ensure the authenticated principal is eligible to receive tokens.

Possible checks include:

- User is active.
- Tenant is active.
- Account is not locked.
- Authentication method is allowed.
- User is permitted to access the selected tenant.
- Session limit has not been exceeded.

Example:

```java
private void validateSessionEligibility(
        AuthenticatedUser user) {

    if (!user.active()) {
        throw new UserNotActiveException(user.userId());
    }

    if (!user.tenantActive()) {
        throw new TenantNotActiveException(user.tenantId());
    }
}
```

Some checks may already occur during credential authentication. Critical checks can still be repeated at the session boundary when needed.

---

# Step 2 — Create a New Token Family

Every new login creates a new family.

```java
UUID familyId = UUID.randomUUID();
```

The family identifies one logical authenticated session.

Examples:

```text
Laptop login      → Family F100
Mobile login      → Family F200
Tablet login      → Family F300
```

Each family rotates independently.

---

# Why a New Family Per Login?

Creating a new family per login provides clear session isolation.

```text
User
  ├── Laptop session → F100
  ├── Mobile session → F200
  └── Tablet session → F300
```

This allows OdinSync to:

- Revoke one device session.
- Preserve sessions on other devices.
- Detect replay within a specific chain.
- Display active sessions accurately.

Reusing the same family across separate logins would incorrectly merge independent sessions.

---

# Step 3 — Issue the Initial Refresh Token

The token issuer generates the raw token and corresponding domain record.

```java
IssuedRefreshToken issuedToken =
        refreshTokenIssuer.issue(
                authenticatedUser.userId(),
                authenticatedUser.tenantId(),
                familyId);
```

Internally:

```text
SecureRandom
    │
    ▼
Raw token R1
    │
    ▼
SHA-256
    │
    ▼
Hash H1
```

The client receives `R1`.

The database receives `H1`.

---

# Step 4 — Add Session Metadata

The domain model can be enriched before persistence.

```java
RefreshToken refreshToken =
        issuedToken.refreshToken()
                .withClientContext(
                        clientContext.deviceName(),
                        clientContext.userAgent(),
                        clientContext.ipAddress());
```

An immutable model may instead accept all metadata during construction.

```java
RefreshToken refreshToken = RefreshToken.issue(
        tokenId,
        userId,
        tenantId,
        familyId,
        tokenHash,
        issuedAt,
        expiresAt,
        deviceName,
        userAgent,
        ipAddress);
```

The exact implementation depends on the domain-model style selected for OdinSync.

---

# Step 5 — Persist the Session

Only the token hash is stored.

```java
refreshTokenStore.save(refreshToken);
```

The persisted record should initially contain:

```text
id                     = token UUID
user_id                = authenticated user
tenant_id              = authenticated tenant
family_id              = new family UUID
token_hash             = SHA-256(raw token)
issued_at               = current UTC time
expires_at              = issued_at + configured TTL
revoked_at              = null
replaced_by_token_id    = null
device_name             = optional
user_agent              = optional
ip_address              = optional
last_used_at            = null or issued_at
```

The initial token is active.

---

# Initial Database Record

Example:

| Field | Value |
|---|---|
| `id` | `T1` |
| `user_id` | `U100` |
| `tenant_id` | `TENANT10` |
| `family_id` | `F100` |
| `token_hash` | `H1` |
| `issued_at` | `2026-07-19T10:00:00Z` |
| `expires_at` | `2026-08-18T10:00:00Z` |
| `revoked_at` | `NULL` |
| `replaced_by_token_id` | `NULL` |

This record becomes the root of the session chain.

---

# Step 6 — Return the Issued Token

The service returns the raw secret and safe metadata.

```java
return new IssuedRefreshToken(
        issuedToken.rawToken(),
        savedRefreshToken);
```

The raw token must not be added to the domain entity or persisted object.

---

# Recommended Implementation

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final RefreshTokenIssuer refreshTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;
    private final SessionEligibilityPolicy sessionEligibilityPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public IssuedRefreshToken createSession(
            AuthenticatedUser authenticatedUser,
            ClientSessionContext clientContext) {

        sessionEligibilityPolicy.validate(authenticatedUser);

        UUID familyId = UUID.randomUUID();

        IssuedRefreshToken issuedToken =
                refreshTokenIssuer.issue(
                        authenticatedUser.userId(),
                        authenticatedUser.tenantId(),
                        familyId);

        RefreshToken refreshToken =
                issuedToken.refreshToken()
                        .withClientContext(clientContext);

        RefreshToken savedToken =
                refreshTokenStore.save(refreshToken);

        eventPublisher.publishEvent(
                new SessionCreatedEvent(
                        savedToken.id(),
                        savedToken.familyId(),
                        savedToken.userId(),
                        savedToken.tenantId(),
                        savedToken.issuedAt(),
                        savedToken.expiresAt()));

        return new IssuedRefreshToken(
                issuedToken.rawToken(),
                savedToken);
    }
}
```

This implementation coordinates the workflow without implementing generation, hashing, or persistence details directly.

---

# Transaction Boundary

Session creation should run within a transaction.

```java
@Transactional
public IssuedRefreshToken createSession(...) {
}
```

This ensures the database record is persisted consistently.

However, one important distinction is required:

- Generating a raw token is an in-memory operation.
- Persisting its hash is a database operation.
- Returning the raw token occurs only after persistence succeeds.

If persistence fails, the transaction rolls back and the raw token must not be returned to the client.

---

# Failure Before Persistence

Example:

```text
Generate R1
    │
    ▼
Hash R1
    │
    ▼
Database insert fails
    │
    ▼
Transaction rolls back
    │
    ▼
No token returned
```

The generated raw token becomes unusable because no corresponding hash exists in the database.

This is safe.

---

# Failure After Persistence but Before Response

A more subtle scenario occurs when:

```text
Database commit succeeds
      │
      ▼
Network response fails
```

The server now contains an active refresh-token record, but the client never receives the raw token.

This creates an orphaned session.

It is not an authentication vulnerability because nobody can use the stored hash directly. However, it may create unnecessary session records.

Possible mitigations include:

- Short cleanup retention for never-used sessions.
- Tracking `last_used_at`.
- Session limits.
- Client retry behavior that performs a fresh login.
- Idempotent login-session creation for selected clients.

For the initial implementation, cleanup and session-management policies are sufficient.

---

# Access Token and Refresh Token Ordering

A login operation must issue both token types.

Two orderings are possible.

## Option A — Access Token First

```text
Generate access token
      │
      ▼
Create refresh session
      │
      ▼
Return both
```

## Option B — Refresh Session First

```text
Create refresh session
      │
      ▼
Generate access token
      │
      ▼
Return both
```

Because JWT generation is normally in-memory and cannot fail under ordinary conditions after startup configuration succeeds, either ordering can work.

A robust design coordinates both inside a single application service and returns nothing unless all required steps succeed.

---

# Recommended Login Token Pair

```java
public record LoginTokenPair(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {

    public LoginTokenPair {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Access token is required");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token is required");
        }

        if (tokenType == null || tokenType.isBlank()) {
            tokenType = "Bearer";
        }
    }
}
```

The HTTP layer may choose not to include the refresh token in the JSON body when browser cookies are used.

---

# Browser Response Strategy

For a browser client, the preferred design is often:

```text
Access Token
  └── Response body

Refresh Token
  └── Secure HttpOnly cookie
```

Example response:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

The raw refresh token is written to a cookie with attributes such as:

```text
HttpOnly
Secure
SameSite
Path=/auth
Max-Age
```

Cookie behavior belongs in the controller or response adapter, not in `RefreshTokenService`.

---

# Native Client Response Strategy

For mobile or desktop clients, the response may contain both tokens:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "opaque-secret",
  "tokenType": "Bearer",
  "accessTokenExpiresAt": "2026-07-19T10:15:00Z",
  "refreshTokenExpiresAt": "2026-08-18T10:00:00Z"
}
```

The client must store the refresh token in platform-provided secure storage.

---

# Session Limits

OdinSync may later enforce a maximum number of active session families per user and tenant.

Example policy:

```text
Maximum active families = 5
```

On a sixth login, possible behaviors include:

- Reject the login.
- Revoke the oldest session.
- Ask the user to terminate another session.
- Allow trusted administrators to configure the limit.

This policy should not be embedded directly into the token generator.

It belongs in a dedicated session policy component.

---

# Session Eligibility Policy

Recommended abstraction:

```java
public interface SessionEligibilityPolicy {

    void validate(AuthenticatedUser authenticatedUser);
}
```

Possible implementation:

```java
@Component
@RequiredArgsConstructor
public class DefaultSessionEligibilityPolicy
        implements SessionEligibilityPolicy {

    private final RefreshTokenStore refreshTokenStore;
    private final SessionPolicyProperties properties;

    @Override
    public void validate(
            AuthenticatedUser authenticatedUser) {

        long activeSessions =
                refreshTokenStore.countActiveFamilies(
                        authenticatedUser.userId(),
                        authenticatedUser.tenantId());

        if (activeSessions >=
                properties.maximumActiveSessions()) {
            throw new SessionLimitExceededException();
        }
    }
}
```

This keeps configurable session policy outside the core issuance workflow.

---

# Multi-Tenant Boundary

Every session must be associated with both:

- `userId`
- `tenantId`

This matters because one user may belong to multiple tenants.

```text
User U100
  ├── Tenant A → family F100
  └── Tenant B → family F200
```

Session queries and revocation operations must include tenant context where required.

A token issued for Tenant A must never be silently reused to establish a session for Tenant B.

---

# Authorization Snapshot

The access token usually contains current claims such as:

- Roles
- Permissions
- Tenant
- Authorization version

The refresh-token record should not duplicate the complete authorization snapshot unless there is a specific audit requirement.

During login:

```text
AuthenticatedUser
      │
      ├── Access token claims
      └── Refresh session identity
```

During future refresh operations, the service should load the latest user authorization data rather than reusing stale claims.

This will be covered in the rotation chapter.

---

# Session-Creation Events

After a successful session creation, OdinSync may publish:

```java
public record SessionCreatedEvent(
        UUID tokenId,
        UUID familyId,
        UUID userId,
        UUID tenantId,
        Instant issuedAt,
        Instant expiresAt
) {
}
```

Potential consumers include:

- Security audit logging
- Login notification service
- Session analytics
- Suspicious-login detection
- Device history

The event must not include the raw refresh token or its hash.

---

# Publish Events After Commit

Publishing an event inside a transaction can cause consumers to act before the transaction commits.

A safer approach is:

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
```

This ensures external actions occur only after session persistence succeeds.

Example:

```java
@Component
public class SessionCreatedAuditListener {

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCreated(
            SessionCreatedEvent event) {

        // Write audit entry or dispatch notification.
    }
}
```

For guaranteed event delivery across failures, OdinSync may later adopt the transactional outbox pattern.

---

# Idempotency Considerations

Ordinary login requests are not necessarily idempotent.

Submitting valid credentials twice may create two independent sessions:

```text
Login request 1 → F100
Login request 2 → F200
```

This may be expected behavior.

For clients that automatically retry requests after network timeouts, OdinSync may later support an idempotency key.

Example:

```text
Idempotency-Key: 7e6e...
```

The server could associate the key with the resulting session response for a short period.

However, storing or replaying raw refresh-token responses requires careful secret handling.

This should be treated as an advanced enhancement rather than part of the initial implementation.

---

# Security Logging

Safe log example:

```text
Session created:
userId=U100
tenantId=TENANT10
familyId=F100
tokenId=T1
deviceName=Chrome on macOS
expiresAt=2026-08-18T10:00:00Z
```

Unsafe log example:

```text
Session created with refreshToken=YL7nqN5...
```

The following values must never be logged:

- Raw refresh token
- Access token
- Authorization header
- Authentication password
- Cookie secret
- Token hash

---

# Error Handling

Possible session-creation failures include:

- User is inactive.
- Tenant is inactive.
- Session limit exceeded.
- Invalid configuration.
- Database write failure.
- Unique token-hash violation.
- Access-token generation failure.

These should map to domain-specific exceptions.

Example:

```java
public class SessionCreationException
        extends RuntimeException {

    public SessionCreationException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
```

Credential failures should remain distinct from session infrastructure failures.

For example:

- Invalid password → `401 Unauthorized`
- Inactive account → `403 Forbidden`
- Session limit exceeded → `409 Conflict` or policy-specific response
- Internal persistence failure → `500 Internal Server Error`

---

# Token-Hash Collision Retry

A token-hash collision is effectively impossible with properly generated high-entropy tokens, but the database unique constraint still protects integrity.

A bounded retry policy may be implemented in the issuer or service.

```java
private static final int MAX_GENERATION_ATTEMPTS = 3;
```

Conceptual flow:

```text
Generate token
    │
    ▼
Insert hash
    │
    ├── Success → return
    │
    └── Unique violation
             │
             ▼
        Generate again
```

The retry must be limited.

Repeated collisions should be treated as a severe operational anomaly.

Care is required because a database transaction may become rollback-only after a constraint violation. Retrying inside the same transaction may not work reliably.

A better approach is to:

- Detect duplicates before save where practical.
- Retry in a separate transaction boundary.
- Or treat collision as fatal because the probability is negligible.

For the initial implementation, failing securely and emitting a critical metric is acceptable.

---

# Complete Sequence Diagram

```text
Client
  │
  │ POST /auth/login
  ▼
LoginController
  │
  │ email + password
  ▼
AuthenticationService
  │
  │ AuthenticationManager.authenticate(...)
  ▼
AuthenticatedUser
  │
  ▼
LoginSessionService
  │
  ├──────────────► AccessTokenIssuer
  │                    │
  │                    └── Signed JWT
  │
  └──────────────► RefreshTokenService
                       │
                       ├── Validate eligibility
                       ├── Create family ID
                       ├── Generate raw token
                       ├── Hash token
                       ├── Persist session
                       └── Return issued token
  │
  ▼
LoginTokenPair
  │
  ▼
Client
```

---

# Unit Testing Strategy

The session-creation method should verify:

- Session policy is checked.
- A new family ID is generated.
- The issuer receives the authenticated user and tenant IDs.
- Client metadata is attached.
- The refresh-token record is persisted.
- The returned raw token is unchanged.
- The stored model never contains the raw token.
- A session-created event is published.
- Persistence failures prevent a successful result.

Example:

```java
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private SessionEligibilityPolicy eligibilityPolicy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RefreshTokenServiceImpl service;

    @Test
    void shouldCreateNewRefreshTokenFamily() {

        AuthenticatedUser user = TestUsers.activeUser();
        ClientSessionContext context =
                TestSessions.defaultContext();

        RefreshToken token = TestTokens.activeToken();
        IssuedRefreshToken issued =
                new IssuedRefreshToken("raw-token", token);

        when(refreshTokenIssuer.issue(
                eq(user.userId()),
                eq(user.tenantId()),
                any(UUID.class)))
                .thenReturn(issued);

        when(refreshTokenStore.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        IssuedRefreshToken result =
                service.createSession(user, context);

        assertThat(result.rawToken())
                .isEqualTo("raw-token");

        verify(eligibilityPolicy).validate(user);
        verify(refreshTokenStore).save(any());
        verify(eventPublisher).publishEvent(any(
                SessionCreatedEvent.class));
    }
}
```

---

# Integration Testing Strategy

An integration test should verify the complete persistence behavior:

1. Create an authenticated user.
2. Call `createSession`.
3. Query the refresh-token table.
4. Verify one record exists.
5. Verify the record has a new family ID.
6. Verify `revoked_at` is null.
7. Verify `replaced_by_token_id` is null.
8. Verify the raw token is not present in the database.
9. Hash the returned raw token.
10. Verify the hash matches the stored value.
11. Verify tenant isolation.
12. Verify timestamps use UTC.

Testcontainers with the production database engine is preferred for locking and schema behavior.

---

# Security Invariants

Login session creation must preserve these invariants:

1. Session creation occurs only after successful authentication.
2. Client input never determines trusted user or tenant identity.
3. Every new login creates a new token family.
4. The initial token is active and has no predecessor.
5. The raw refresh token is returned only to the client.
6. Only the token hash is persisted.
7. Token expiration uses validated configuration.
8. Tenant context is mandatory.
9. Raw tokens and hashes are excluded from logs and events.
10. No response is returned when persistence fails.
11. Session metadata must not influence authorization decisions.
12. Access-token and refresh-token issuance are coordinated by an application service.

---

# Common Mistakes

## Creating the family during every refresh

A family is created only during a new login.

Rotation preserves the existing family ID.

---

## Trusting tenant ID from the request

Tenant identity must come from authenticated server-side context.

---

## Persisting the raw refresh token

Only a cryptographic hash should be stored.

---

## Returning JPA entities

The service should return domain models or dedicated response objects.

---

## Putting cookie logic inside the service

HTTP cookie configuration belongs in the web adapter.

---

## Logging token values

Neither raw tokens nor hashes should appear in logs, metrics, traces, or events.

---

## Mixing password authentication with token persistence

Credential verification and session creation should be separate operations coordinated by a higher-level application service.

---

# Design Decisions in OdinSync

OdinSync adopts the following login-session design:

- Credential verification remains in `AuthenticationService`.
- `LoginSessionService` coordinates access-token and refresh-token issuance.
- Every successful login creates a new refresh-token family.
- `RefreshTokenService` creates and persists the initial refresh session.
- Raw refresh tokens are never stored.
- Client metadata is optional and non-authoritative.
- User and tenant identity come only from trusted authentication state.
- Session creation runs inside a transaction.
- Domain events exclude credential material.
- Browser cookie handling remains outside the application service.

---

# Improvements and Enhancements Recorded

Add the following items to the final Codex implementation task:

- Introduce a `LoginSessionService` to coordinate access-token and refresh-token issuance.
- Introduce a `ClientSessionContext` value object for device, user-agent, and IP metadata.
- Introduce a `SessionEligibilityPolicy` abstraction.
- Add configurable maximum active session families per user and tenant.
- Add `SessionCreatedEvent` without token values.
- Handle session-created consumers with `AFTER_COMMIT` listeners.
- Evaluate transactional outbox support for guaranteed security-event delivery.
- Add cleanup logic for sessions created successfully when the response is never received.
- Add metrics for session creation success and failure.
- Add a critical metric for unexpected token-hash uniqueness violations.
- Evaluate idempotency support for retrying native clients.
- Add browser-specific secure-cookie response handling.
- Add mobile and desktop secure-storage guidance.
- Add integration tests using the production database engine through Testcontainers.
- Ensure tenant-aware indexes and session-count queries.
- Ensure the access token includes the latest `authorization_version`.
- Preserve the plan to validate current authorization state during future refresh operations.

---

# Looking Ahead

The next section implements the main refresh-token operation:

**Part 11.3 – Refresh Token Rotation**

It will cover:

- Hashing the submitted token.
- Loading the row with a pessimistic lock.
- Validating expiration and revocation state.
- Loading current user and tenant authorization data.
- Issuing a new access token.
- Creating a replacement refresh token.
- Linking the old token to the replacement.
- Preserving the family ID.
- Committing the entire rotation atomically.
- Handling concurrent requests safely.

---

# Summary

Login session creation establishes the root of a refresh-token family.

After successful credential authentication, OdinSync creates a new family, generates a high-entropy opaque token, stores only its hash, and returns the raw value to the client.

The operation remains separated from password authentication, JWT implementation details, HTTP cookies, and persistence internals.

This creates a clean and secure foundation for refresh-token rotation.