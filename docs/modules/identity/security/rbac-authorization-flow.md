# RBAC Authorization Flow using Spring Security OAuth2 Resource Server

## 1. Purpose

This document explains how Role-Based Access Control (RBAC) is implemented in OdinSync after successful JWT authentication.

The authentication flow has already been completed:

```text
Client
    ↓
BearerTokenAuthenticationFilter
    ↓
JwtAuthenticationProvider
    ↓
JwtDecoder
    ↓
JwtAuthenticationConverter
    ↓
JwtAuthenticationToken
    ↓
SecurityContextHolder
```

At this point the user is authenticated.

RBAC answers the next question:

> **Is this authenticated user allowed to perform the requested operation?**

Authentication verifies **identity**.

Authorization verifies **permission**.

---

# 2. Authentication vs Authorization

## Authentication

Authentication answers:

```text
Who are you?
```

Example:

```text
Bearer Token
        ↓
JWT Signature Verified
        ↓
JWT Trusted
        ↓
Authenticated User
```

Result:

```text
SecurityContextHolder
        ↓
JwtAuthenticationToken
```

---

## Authorization

Authorization answers:

```text
What are you allowed to do?
```

Example:

```text
Authenticated User
        ↓
ROLE_OWNER
        ↓
Access OWNER endpoint
```

or

```text
Authenticated User
        ↓
ROLE_MEMBER
        ↓
Access ADMIN endpoint
        ↓
403 Forbidden
```

---

# 3. RBAC Architecture

```text
                JWT Authentication
                        │
                        ▼
             JwtAuthenticationToken
                        │
                        ▼
             SecurityContextHolder
                        │
                        ▼
              AuthorizationManager
                        │
         ┌──────────────┴──────────────┐
         │                             │
         ▼                             ▼
 Request-Level Rules           Method-Level Rules
 authorizeHttpRequests()       @PreAuthorize()
         │                             │
         └──────────────┬──────────────┘
                        │
                        ▼
                 Controller / Use Case
```

---

# 4. Organization Role Model

The domain contains business roles.

```java
public enum OrganizationRole {

    OWNER,

    ADMIN,

    MEMBER
}
```

Notice these are business roles.

They **must not** contain Spring Security prefixes.

Correct:

```text
OWNER
ADMIN
MEMBER
```

Wrong:

```text
ROLE_OWNER
ROLE_ADMIN
```

Spring-specific concerns belong only in the security layer.

---

# 5. JWT Role Claim

During login the JWT contains:

```json
{
    "sub":"7fb9...",
    "tenant_id":"22ad...",
    "email":"owner@odinsync.com",
    "roles":[
        "OWNER"
    ],
    "iss":"odinsync-platform",
    "iat":1784310000,
    "exp":1784310900
}
```

Roles are stored as:

```json
[
    "OWNER",
    "ADMIN"
]
```

Never as:

```text
OWNER,ADMIN
```

because Spring expects a collection.

---

# 6. Why ROLE_ Prefix Exists

Spring Security internally treats:

```java
hasRole("OWNER")
```

as

```text
ROLE_OWNER
```

Internally it performs:

```text
ROLE_ + OWNER
```

Therefore our converter transforms:

```text
OWNER
```

into

```text
ROLE_OWNER
```

If JWT already contains

```text
ROLE_OWNER
```

the converter must avoid producing

```text
ROLE_ROLE_OWNER
```

---

# 7. JwtAuthenticationConverter

The converter receives a validated Jwt.

```text
Validated Jwt
        ↓
roles claim
        ↓
ROLE_ conversion
        ↓
GrantedAuthority
        ↓
JwtAuthenticationToken
```

Responsibilities:

- Read `roles` claim.
- Ignore null values.
- Trim whitespace.
- Convert to uppercase.
- Add `ROLE_` prefix.
- Remove duplicates.
- Create `JwtAuthenticationToken`.

The converter **does not**:

- verify signatures;
- query the database;
- validate expiration;
- load `UserDetails`.

Those responsibilities belong to `JwtDecoder`.

---

# 8. Authentication Object after Conversion

After conversion:

```text
JwtAuthenticationToken

Authenticated = true

Principal = Jwt

Authorities

ROLE_OWNER

ROLE_ADMIN
```

Stored inside:

```text
SecurityContextHolder
```

---

# 9. SecurityContextHolder

After authentication:

```text
SecurityContextHolder
        │
        ▼
SecurityContext
        │
        ▼
JwtAuthenticationToken
        │
 ├── Principal
 ├── Authorities
 └── Authenticated=true
```

Controllers and services no longer need to parse JWTs.

Spring injects the authenticated user automatically.

Example:

```java
@AuthenticationPrincipal Jwt jwt
```

---

# 10. Request-Level Authorization

Configured in:

```java
.authorizeHttpRequests(...)
```

Example:

```java
.requestMatchers("/api/v1/admin/**")
.hasRole("ADMIN")
```

Flow:

```text
Incoming Request
        ↓
AuthorizationFilter
        ↓
Requires ROLE_ADMIN
        ↓
Authorities checked
```

If authority exists:

```text
Controller executes
```

Otherwise:

```text
403 Forbidden
```

---

# 11. Method-Level Authorization

Enabled using:

```java
@EnableMethodSecurity
```

Now Spring supports:

```java
@PreAuthorize("hasRole('OWNER')")
```

Example:

```java
@PreAuthorize("hasRole('OWNER')")
public void updateOrganizationSettings() {

}
```

Before the method executes:

```text
MethodSecurityInterceptor
        ↓
Evaluate Expression
        ↓
ROLE_OWNER exists?
```

If yes

```text
Method executes
```

Otherwise

```text
403 Forbidden
```

---

# 12. Request-Level vs Method-Level

## Request-Level

```java
.requestMatchers("/admin/**")
.hasRole("ADMIN")
```

Protects URLs.

Best for:

- Public APIs
- Admin routes
- Health endpoints

---

## Method-Level

```java
@PreAuthorize(...)
```

Protects business operations.

Best for:

- Service methods
- Use Cases
- Domain operations

OdinSync should use both.

---

# 13. Authorization Flow

```text
HTTP Request
        │
        ▼
SecurityFilterChain
        │
        ▼
BearerTokenAuthenticationFilter
        │
        ▼
JwtAuthenticationProvider
        │
        ▼
JwtDecoder
        │
        ▼
JWT Validated
        │
        ▼
JwtAuthenticationConverter
        │
        ▼
ROLE_* Authorities
        │
        ▼
SecurityContextHolder
        │
        ▼
AuthorizationFilter
        │
        ▼
Request Matchers
        │
        ▼
Method Security
        │
        ▼
Controller
```

---

# 14. Successful Authorization

Token:

```json
{
    "roles":[
        "OWNER"
    ]
}
```

Endpoint:

```java
@PreAuthorize("hasRole('OWNER')")
```

Flow:

```text
JWT Valid
        ↓
ROLE_OWNER
        ↓
Authorization succeeds
        ↓
Controller executes
        ↓
200 OK
```

---

# 15. Authorization Failure

Token:

```json
{
    "roles":[
        "MEMBER"
    ]
}
```

Endpoint:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Flow:

```text
JWT Valid
        ↓
ROLE_MEMBER
        ↓
ADMIN required
        ↓
AccessDeniedException
        ↓
AccessDeniedHandler
        ↓
403 Forbidden
```

Authentication succeeds.

Authorization fails.

---

# 16. 401 vs 403

## 401 Unauthorized

Authentication failed.

Examples:

- Missing token
- Expired token
- Invalid signature
- Wrong issuer
- Malformed JWT

Flow:

```text
JwtDecoder
        ↓
Authentication fails
        ↓
AuthenticationEntryPoint
        ↓
401
```

---

## 403 Forbidden

Authentication succeeded.

Authorization failed.

Flow:

```text
JWT Valid
        ↓
ROLE_USER
        ↓
ROLE_ADMIN required
        ↓
AccessDeniedHandler
        ↓
403
```

Remember:

```text
401

"I don't know who you are."
```

```text
403

"I know who you are.

You are not allowed."
```

---

# 17. Temporary Test Endpoints

During RBAC development:

```text
/api/v1/security-test/authenticated

/api/v1/security-test/owner

/api/v1/security-test/admin

/api/v1/security-test/member

/api/v1/security-test/owner-or-admin
```

These endpoints verify authorization behaviour before applying RBAC to real business endpoints.

They should later be removed.

---

# 18. Postman Verification

| Test | Expected |
|-------|----------|
| OWNER → authenticated | 200 |
| OWNER → owner | 200 |
| OWNER → admin | 403 |
| MEMBER → member | 200 |
| MEMBER → owner | 403 |
| ADMIN → admin | 200 |
| No token | 401 |
| Invalid token | 401 |

---

# 19. SecurityContext Example

When debugging:

```java
Authentication authentication =
    SecurityContextHolder
        .getContext()
        .getAuthentication();
```

Expected:

```text
Type

JwtAuthenticationToken

Principal

Jwt

Authenticated

true

Authorities

ROLE_OWNER
```

---

# 20. Future Evolution

Current implementation uses role-based authorization.

Future enhancements include:

```text
Role
        ↓
Permissions
        ↓
Authorities
        ↓
Fine-Grained Authorization
```

Example:

```text
CUSTOMER_READ

CUSTOMER_WRITE

ORDER_APPROVE

USER_INVITE

PAYMENT_REFUND
```

Later phases will introduce:

- Permission-based authorization
- Refresh tokens
- Authorization versioning
- Immediate permission revocation
- Redis-backed authorization cache
- Tenant membership validation
- Attribute-Based Access Control (ABAC) where required

---

# 21. Complete RBAC Flow

```text
                    Client
                      │
                      ▼
           BearerTokenAuthenticationFilter
                      │
                      ▼
            JwtAuthenticationProvider
                      │
                      ▼
                  JwtDecoder
                      │
        Signature + Claims Verified
                      │
                      ▼
        OdinSyncJwtAuthenticationConverter
                      │
          ROLE_* Authorities Created
                      │
                      ▼
             JwtAuthenticationToken
                      │
                      ▼
            SecurityContextHolder
                      │
                      ▼
            AuthorizationFilter
                      │
             Request-Level Rules
                      │
                      ▼
           Method Security (@PreAuthorize)
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
   Authorization OK          Authorization Failed
         │                         │
         ▼                         ▼
 Controller Executes        AccessDeniedHandler
         │                         │
         ▼                         ▼
      HTTP 200               HTTP 403
```

---

# 22. Key Takeaways

- JWT authentication and RBAC are separate responsibilities.
- `JwtDecoder` authenticates the token.
- `JwtAuthenticationConverter` converts business roles into Spring authorities.
- `SecurityContextHolder` stores the authenticated identity.
- `AuthorizationFilter` evaluates URL-based rules.
- `@PreAuthorize` evaluates business-level rules.
- Missing or invalid authentication returns **401**.
- Authenticated users without sufficient authority receive **403**.
- Domain roles remain framework-independent (`OWNER`, `ADMIN`, `MEMBER`), while Spring-specific `ROLE_` authorities are created only inside the security layer.