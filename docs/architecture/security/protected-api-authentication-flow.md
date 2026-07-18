# OAuth2 Resource Server Protected API Authentication Flow

## 1. Purpose

This document explains how OdinSync protects APIs using Spring Security's OAuth2 Resource Server support.

OdinSync does **not** use a custom `OncePerRequestFilter` to parse and validate JWTs. Instead, it configures Spring Security's standard resource-server infrastructure:

```java
.oauth2ResourceServer(resourceServer ->
        resourceServer.jwt(jwt ->
                jwt.jwtAuthenticationConverter(
                        odinSyncJwtAuthenticationConverter
                )
        )
)
```

That configuration automatically connects the components required to:

- read the `Authorization` header;
- extract the bearer token;
- verify the JWT signature;
- validate issuer and timestamps;
- convert JWT claims into Spring Security authorities;
- create an authenticated `Authentication`;
- store it in `SecurityContextHolder`;
- authorize the request before the controller executes.

This document focuses on protected API requests. Login credential authentication is documented separately.

---

## 2. Resource Server Meaning

In OAuth2 terminology, a **resource server** is an API that accepts access tokens and protects application resources.

For OdinSync:

```text
Login endpoint
    → issues an access token

Protected API
    → receives and validates the access token
```

Examples of protected resources:

```text
GET  /api/v1/users/me
GET  /api/v1/customers
POST /api/v1/orders
GET  /api/v1/invoices
```

The client sends the token using:

```http
Authorization: Bearer <access-token>
```

---

## 3. OdinSync Security Configuration

A representative OdinSync configuration is:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OdinSyncJwtAuthenticationConverter jwtAuthenticationConverter,
            OdinSyncAuthenticationEntryPoint authenticationEntryPoint,
            OdinSyncAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer
                                .jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(
                                                jwtAuthenticationConverter
                                        )
                                )
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .build();
    }
}
```

The important line is:

```java
.oauth2ResourceServer(resourceServer ->
        resourceServer.jwt(...)
)
```

It tells Spring Security:

> Authenticate bearer tokens as JWTs using the configured `JwtDecoder`.

---

## 4. What Spring Configures Automatically

OdinSync does not manually instantiate or register the protected-request components.

Spring Security configures the equivalent of:

```text
BearerTokenAuthenticationFilter
        ↓
AuthenticationManager
        ↓
JwtAuthenticationProvider
        ↓
JwtDecoder
        ↓
JwtAuthenticationConverter
```

The resource-server DSL wires `BearerTokenAuthenticationFilter` into the `SecurityFilterChain`. That filter extracts bearer tokens and delegates authentication to an `AuthenticationManager`.

Conceptually, Spring performs configuration similar to:

```java
JwtAuthenticationProvider provider =
        new JwtAuthenticationProvider(jwtDecoder);

provider.setJwtAuthenticationConverter(
        jwtAuthenticationConverter
);

AuthenticationManager authenticationManager =
        new ProviderManager(provider);

BearerTokenAuthenticationFilter filter =
        new BearerTokenAuthenticationFilter(
                authenticationManager
        );
```

This is conceptual code. Spring creates and connects the actual components internally.

---

## 5. Why No Custom JWT Filter Is Required

A custom implementation normally contains code such as:

```java
String authHeader =
        request.getHeader("Authorization");

String jwt =
        authHeader.substring(7);

String username =
        jwtService.extractUsername(jwt);

UserDetails userDetails =
        userDetailsService.loadUserByUsername(username);

SecurityContextHolder
        .getContext()
        .setAuthentication(authentication);
```

With OAuth2 Resource Server, Spring already provides standardized components for those responsibilities.

| Responsibility | Custom filter | Resource Server |
|---|---|---|
| Read `Authorization` | Custom code | `BearerTokenAuthenticationFilter` |
| Extract bearer token | Custom code | Bearer token resolver |
| Wrap authentication request | Usually implicit | `BearerTokenAuthenticationToken` |
| Route authentication | Often bypassed | `AuthenticationManager` |
| Verify JWT | Custom service | `JwtDecoder` |
| JWT provider | Custom logic | `JwtAuthenticationProvider` |
| Map roles | Custom code | `JwtAuthenticationConverter` |
| Set security context | Custom code | Spring Security |
| Return 401 | Custom code | `AuthenticationEntryPoint` |
| Return 403 | Custom code | `AccessDeniedHandler` |

Using the resource server prevents duplicate parsing and keeps OdinSync aligned with Spring Security's authentication architecture.

---

## Role-Based Authorization

OdinSync access tokens carry organization roles as plain domain values:

```json
{
  "roles": [
    "OWNER",
    "ADMIN"
  ]
}
```

The domain role values are not stored with Spring's `ROLE_` prefix. During protected API authentication, `OdinSyncJwtAuthenticationConverter` converts the JWT `roles` claim into Spring Security authorities:

| JWT role | Spring authority |
|---|---|
| `OWNER` | `ROLE_OWNER` |
| `ADMIN` | `ROLE_ADMIN` |
| `MEMBER` | `ROLE_MEMBER` |

The converter trims values, ignores blanks and nulls, uppercases roles, avoids double-prefixing `ROLE_`, and deduplicates authorities. Controllers do not parse bearer tokens or inspect authorization headers.

Request-level authorization is configured in `SecurityConfig` for broad endpoint groups, for example temporary RBAC verification endpoints:

```java
.requestMatchers("/api/v1/security-test/admin").hasRole("ADMIN")
.requestMatchers("/api/v1/security-test/owner").hasRole("OWNER")
.requestMatchers("/api/v1/security-test/owner-or-admin").hasAnyRole("OWNER", "ADMIN")
```

Method-level authorization is enabled with `@EnableMethodSecurity`, allowing use cases and controllers to enforce rules with:

```java
@PreAuthorize("hasRole('OWNER')")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
@PreAuthorize("hasAuthority('ROLE_MEMBER')")
```

Authentication and authorization failures are intentionally distinct:

| Scenario | Result | Handler |
|---|---|---|
| Missing token | `401 Unauthorized` | `OdinSyncAuthenticationEntryPoint` |
| Malformed token | `401 Unauthorized` | `OdinSyncAuthenticationEntryPoint` |
| Invalid signature | `401 Unauthorized` | `OdinSyncAuthenticationEntryPoint` |
| Expired token | `401 Unauthorized` | `OdinSyncAuthenticationEntryPoint` |
| Wrong issuer | `401 Unauthorized` | `OdinSyncAuthenticationEntryPoint` |
| Valid token, missing role | `403 Forbidden` | `OdinSyncAccessDeniedHandler` |

After successful authentication, `SecurityContextHolder` contains a `JwtAuthenticationToken`. Its principal is the validated `Jwt`, its name is the JWT subject, and its authorities are converted from the `roles` claim.

Postman checks:

1. Send an OWNER token to `GET /api/v1/security-test/authenticated`: expect `200 OK`.
2. Send an OWNER token to `GET /api/v1/security-test/owner`: expect `200 OK`.
3. Send an OWNER token to `GET /api/v1/security-test/admin`: expect `403 Forbidden`.
4. Send an OWNER token to `GET /api/v1/security-test/owner-or-admin`: expect `200 OK`.
5. Send no token to `GET /api/v1/security-test/admin`: expect `401 Unauthorized`.

---

## 6. High-Level Protected API Flow

```mermaid
flowchart TD
    A[Client sends protected API request] --> B[Tomcat]
    B --> C[Spring Security FilterChain]
    C --> D[BearerTokenAuthenticationFilter]
    D --> E{Bearer token present?}

    E -->|No| F[Continue without authenticated user]
    F --> G[AuthorizationFilter]
    G --> H[Authentication required]
    H --> I[AuthenticationEntryPoint]
    I --> J[401 Unauthorized]

    E -->|Yes| K[Create BearerTokenAuthenticationToken]
    K --> L[AuthenticationManager]
    L --> M[JwtAuthenticationProvider]
    M --> N[JwtDecoder]
    N --> O{JWT valid?}

    O -->|No| I
    O -->|Yes| P[Validated Jwt]
    P --> Q[JwtAuthenticationConverter]
    Q --> R[JwtAuthenticationToken]
    R --> S[SecurityContextHolder]
    S --> G
    G --> T{Authorization granted?}
    T -->|No| U[AccessDeniedHandler]
    U --> V[403 Forbidden]
    T -->|Yes| W[DispatcherServlet]
    W --> X[Controller]
```

---

## 7. Step 1 — Client Sends the Access Token

After successful login, the client receives an access token.

Example response:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-07-18T10:15:00Z"
}
```

The client includes it in every protected request:

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

The token must not be sent as:

```text
Query parameter
Request body
Cookie, unless a deliberate cookie-based design exists
```

The standard location is the `Authorization` header.

---

## 8. Step 2 — Request Enters the Security Filter Chain

The request first reaches the servlet container, normally Tomcat.

```text
Client
    ↓
Tomcat
    ↓
Spring Security FilterChain
    ↓
DispatcherServlet
    ↓
Controller
```

Spring Security executes before the controller.

Java security configuration creates a servlet filter named:

```text
springSecurityFilterChain
```

Internally, `FilterChainProxy` selects the matching `SecurityFilterChain` and runs its filters.

A simplified filter order is:

```text
SecurityContextHolderFilter
CorsFilter
BearerTokenAuthenticationFilter
AnonymousAuthenticationFilter
ExceptionTranslationFilter
AuthorizationFilter
```

The exact chain may contain additional filters.

---

## 9. Step 3 — BearerTokenAuthenticationFilter Extracts the Token

The resource-server configuration automatically adds:

```text
BearerTokenAuthenticationFilter
```

The filter checks the request for:

```http
Authorization: Bearer <token>
```

Conceptually:

```java
String header =
        request.getHeader(HttpHeaders.AUTHORIZATION);
```

It then resolves the bearer token.

Input:

```text
Bearer eyJhbGciOiJSUzI1NiJ9...
```

Extracted value:

```text
eyJhbGciOiJSUzI1NiJ9...
```

The filter does not yet trust the token.

---

## 10. Step 4 — Missing Token

If there is no bearer token:

```http
GET /api/v1/users/me
```

the filter cannot authenticate the user.

Because the endpoint is configured as:

```java
.anyRequest().authenticated()
```

the later authorization step rejects access.

Flow:

```text
No Authorization header
    ↓
No authenticated Authentication
    ↓
AuthorizationFilter rejects request
    ↓
AuthenticationEntryPoint
    ↓
401 Unauthorized
```

The controller is not invoked.

Example response:

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication is required",
  "timestamp": "2026-07-18T10:00:00Z"
}
```

---

## 11. Step 5 — BearerTokenAuthenticationToken

When the token exists, Spring wraps it in:

```java
BearerTokenAuthenticationToken
```

Conceptual state:

```text
token         = eyJhbGciOiJSUzI1NiJ9...
authenticated = false
principal     = not established
authorities   = empty
```

This object is an authentication request.

It means:

> Validate this bearer token and determine the authenticated identity.

It is not the final authentication object.

---

## 12. Step 6 — AuthenticationManager

`BearerTokenAuthenticationFilter` passes the request to:

```java
authenticationManager.authenticate(
        bearerTokenAuthenticationToken
);
```

The common implementation is:

```text
ProviderManager
```

`ProviderManager` examines its configured providers and selects one supporting the incoming authentication type.

Conceptually:

```java
for (AuthenticationProvider provider : providers) {
    if (provider.supports(authentication.getClass())) {
        return provider.authenticate(authentication);
    }
}
```

For:

```text
BearerTokenAuthenticationToken
```

Spring selects:

```text
JwtAuthenticationProvider
```

---

## 13. Login Provider Versus Protected-Request Provider

OdinSync uses different providers for different authentication mechanisms.

### Login request

```text
UsernamePasswordAuthenticationToken
    ↓
DaoAuthenticationProvider
    ↓
OdinSyncUserDetailsService
    ↓
PasswordEncoder
```

### Protected API request

```text
BearerTokenAuthenticationToken
    ↓
JwtAuthenticationProvider
    ↓
JwtDecoder
    ↓
JwtAuthenticationConverter
```

The provider is selected based on the `Authentication` implementation.

---

## 14. Step 7 — JwtAuthenticationProvider

`JwtAuthenticationProvider` has two primary responsibilities:

```text
1. Decode and validate the raw JWT
2. Convert the validated Jwt into Authentication
```

Conceptually:

```java
Jwt jwt = jwtDecoder.decode(rawToken);

AbstractAuthenticationToken authentication =
        jwtAuthenticationConverter.convert(jwt);
```

If the decoder rejects the JWT, authentication stops.

---

## 15. Step 8 — JwtDecoder

OdinSync exposes a `JwtDecoder` bean:

```java
@Bean
JwtDecoder jwtDecoder(
        KeyPair jwtKeyPair,
        OdinSyncJwtProperties properties
) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withPublicKey(
                    (RSAPublicKey) jwtKeyPair.getPublic()
            )
            .signatureAlgorithm(
                    SignatureAlgorithm.RS256
            )
            .build();

    decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(
                    properties.issuer()
            )
    );

    return decoder;
}
```

The decoder validates the token using:

- RSA public key
- RS256 algorithm requirement
- Expected issuer
- Expiration and timestamp validators

---

## 16. JWT Structure

A signed JWT contains three Base64URL-encoded parts:

```text
header.payload.signature
```

Example:

```text
eyJhbGciOiJSUzI1NiJ9
.
eyJzdWIiOiI3ZmI5Iiwicm9sZXMiOlsiT1dORVIiXX0
.
X7k92...
```

### Header

```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

### Payload

```json
{
  "iss": "odinsync-platform",
  "sub": "7fb9...",
  "tenant_id": "22ad...",
  "email": "owner@odinsync.com",
  "roles": ["OWNER"],
  "iat": 1784310000,
  "exp": 1784310900,
  "jti": "584c..."
}
```

### Signature

The signature is generated from:

```text
Base64Url(header)
    +
"."
    +
Base64Url(payload)
```

using the RSA private key.

---

## 17. Step 9 — Structural Parsing

The decoder first confirms that the token is structurally valid.

Expected:

```text
header.payload.signature
```

Malformed examples:

```text
abc
abc.def
Bearer
```

If parsing fails:

```text
JwtDecoder throws
    ↓
Authentication fails
    ↓
AuthenticationEntryPoint
    ↓
401 Unauthorized
```

---

## 18. Step 10 — Algorithm Validation

OdinSync configures:

```java
.signatureAlgorithm(SignatureAlgorithm.RS256)
```

The JWT header must declare:

```json
{
  "alg": "RS256"
}
```

Unexpected algorithms such as:

```text
HS256
RS512
none
```

are rejected.

The server does not blindly accept whichever algorithm is declared by the incoming token.

---

## 19. Step 11 — RSA Signature Verification

During login, the JWT encoder signs the token with the private key:

```text
Header + Payload
    ↓
RSA private key
    ↓
Signature
```

During a protected request, the decoder verifies it using the corresponding public key:

```text
Header + Payload + Signature
    ↓
RSA public key
    ↓
Signature valid or invalid
```

### Valid signature

The token was signed using the matching private key, and its signed content has not changed.

### Invalid signature

One of the following occurred:

- payload was modified;
- header was modified;
- token was signed using another private key;
- signature was corrupted;
- the server is using the wrong public key.

The request returns `401`.

---

## 20. Why Claims Cannot Be Safely Modified

Original claim:

```json
{
  "roles": ["USER"]
}
```

Attacker changes it to:

```json
{
  "roles": ["ADMIN"]
}
```

The Base64URL payload can be changed, but the attacker cannot create a new valid signature without the private key.

```text
Modified payload
    ↓
Original signature no longer matches
    ↓
Public-key verification fails
    ↓
401 Unauthorized
```

JWT payloads are readable, not secret. Their integrity is protected by the signature.

Never put secrets such as raw passwords inside JWT claims.

---

## 21. Step 12 — Issuer Validation

The token contains:

```json
{
  "iss": "odinsync-platform"
}
```

The decoder expects:

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
```

Validation:

```text
Token issuer == configured issuer
```

If the token says:

```json
{
  "iss": "unknown-system"
}
```

it is rejected.

Issuer validation ensures the token belongs to the expected issuing system.

---

## 22. Step 13 — Expiration Validation

The token includes:

```json
{
  "exp": 1784310900
}
```

Validation:

```text
current time < exp
```

If:

```text
current time >= exp
```

the token is expired.

Even when its signature remains valid, the token is no longer usable.

Response:

```http
401 Unauthorized
```

---

## 23. Step 14 — Not-Before Validation

When present:

```json
{
  "nbf": 1784310060
}
```

the token cannot be accepted before that time.

Validation:

```text
current time >= nbf
```

If the current time is earlier, authentication fails.

---

## 24. Step 15 — Issued-At Claim

The token may contain:

```json
{
  "iat": 1784310000
}
```

`iat` records when the token was issued.

It is useful for:

- auditing;
- token-age rules;
- revocation strategies;
- debugging;
- authorization-version policies.

The main expiration control remains `exp`.

---

## 25. Clock Skew

Distributed systems may have small clock differences.

```text
Identity server: 10:00:00
API server:      10:00:03
```

JWT validation may allow a limited clock skew.

Production systems should keep hosts synchronized using reliable time synchronization.

An excessive skew allowance weakens timestamp enforcement.

---

## 26. Step 16 — Validated Jwt Object

When all configured validations pass, `JwtDecoder` returns:

```java
Jwt
```

It contains:

```text
Original token value
Headers
Claims
Subject
Issued-at timestamp
Expiration timestamp
```

Examples:

```java
jwt.getSubject();

jwt.getClaimAsString("tenant_id");

jwt.getClaimAsString("email");

jwt.getClaimAsStringList("roles");

jwt.getExpiresAt();
```

At this point, the claims are trusted because signature and configured validators succeeded.

The `Jwt` object is not yet the final `Authentication`.

---

## 27. Step 17 — JwtAuthenticationConverter

`JwtAuthenticationProvider` passes the validated `Jwt` to a converter.

OdinSync may implement:

```java
@Component
public class OdinSyncJwtAuthenticationConverter
        implements Converter<
                Jwt,
                AbstractAuthenticationToken
        > {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles =
                jwt.getClaimAsStringList("roles");

        Collection<GrantedAuthority> authorities =
                roles == null
                        ? List.of()
                        : roles.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(role -> !role.isBlank())
                                .map(String::toUpperCase)
                                .map(role ->
                                        role.startsWith("ROLE_")
                                                ? role
                                                : "ROLE_" + role
                                )
                                .distinct()
                                .map(SimpleGrantedAuthority::new)
                                .toList();

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
    }
}
```

Its responsibility is to convert claims into Spring Security identity and authorities.

---

## 28. Role Conversion

Input JWT claim:

```json
{
  "roles": ["OWNER", "ADMIN"]
}
```

Converted authorities:

```text
ROLE_OWNER
ROLE_ADMIN
```

Spring represents authorities using:

```java
SimpleGrantedAuthority
```

The prefix matters because:

```java
hasRole("OWNER")
```

checks for:

```text
ROLE_OWNER
```

Whereas:

```java
hasAuthority("OWNER")
```

checks for exactly:

```text
OWNER
```

OdinSync should use one consistent convention.

---

## 29. Missing Roles

A missing role claim should not crash authentication.

Example:

```json
{
  "sub": "7fb9...",
  "tenant_id": "22ad..."
}
```

Recommended result:

```text
Authenticated identity
Authorities = empty
```

The user can access endpoints requiring only `.authenticated()`, but not role-restricted endpoints.

---

## 30. Step 18 — JwtAuthenticationToken

The converter returns:

```java
JwtAuthenticationToken
```

Conceptual state:

```text
principal     = validated Jwt
name          = JWT subject
authorities   = ROLE_OWNER, ROLE_ADMIN
authenticated = true
```

Example:

```java
Authentication authentication =
        new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
```

This is the final authenticated identity for the protected request.

---

## 31. Login Principal Versus Protected-Request Principal

### During login

```text
Authentication type:
UsernamePasswordAuthenticationToken

Principal:
OdinSyncUserDetails
```

### During protected API authentication

```text
Authentication type:
JwtAuthenticationToken

Principal:
Jwt
```

The protected-request flow does not normally recreate `OdinSyncUserDetails`.

---

## 32. Step 19 — SecurityContextHolder

After successful authentication, `BearerTokenAuthenticationFilter` creates a `SecurityContext` and stores the authentication.

Conceptually:

```java
SecurityContext context =
        SecurityContextHolder.createEmptyContext();

context.setAuthentication(authentication);

SecurityContextHolder.setContext(context);
```

Result:

```text
SecurityContextHolder
└── SecurityContext
    └── JwtAuthenticationToken
        ├── Jwt principal
        ├── authenticated = true
        └── authorities = ROLE_OWNER
```

Spring Security stores the details of the currently authenticated identity in `SecurityContextHolder`.

---

## 33. Request-Scoped and Thread-Local Behavior

By default, servlet-based Spring Security associates the context with the current request thread.

```text
Request thread A
    → User A Authentication

Request thread B
    → User B Authentication
```

At request completion, Spring clears the context.

This prevents authentication from leaking when container threads are reused.

Application code should not manually retain the `Authentication` beyond the request.

---

## 34. Stateless Authentication

OdinSync configures:

```java
SessionCreationPolicy.STATELESS
```

This means:

- no server-side login session;
- no persisted `SecurityContext` in an HTTP session;
- every protected request must carry an access token;
- the token is revalidated for every request.

Lifecycle:

```text
Request starts
    ↓
JWT validated
    ↓
SecurityContext created
    ↓
Controller executes
    ↓
Request completes
    ↓
SecurityContext cleared
```

The next request repeats the process.

---

## 35. Step 20 — AuthorizationFilter

After authentication, Spring runs request authorization.

Rules are configured by:

```java
.authorizeHttpRequests(...)
```

Example:

```java
.anyRequest().authenticated()
```

This asks:

> Does the current `SecurityContext` contain an authenticated `Authentication`?

If yes, the request can proceed.

Role-specific example:

```java
.requestMatchers("/api/v1/admin/**")
.hasRole("ADMIN")
```

This asks:

> Does the current authentication contain `ROLE_ADMIN`?

`AuthorizationFilter` uses an `AuthorizationManager` to apply these rules.

---

## 36. Authentication Versus Authorization

Authentication answers:

```text
Who is this caller?
```

Authorization answers:

```text
May this caller access this operation?
```

Examples:

### Authentication failure

```text
Missing or invalid token
    → 401 Unauthorized
```

### Authorization failure

```text
Valid JWT
Role = USER
Endpoint requires ADMIN
    → 403 Forbidden
```

---

## 37. Step 21 — Controller Execution

Only after authentication and authorization succeed does the request reach the controller.

Example:

```java
@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {

    @GetMapping("/me")
    CurrentUserResponse currentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return new CurrentUserResponse(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(
                        jwt.getClaimAsString("tenant_id")
                ),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsStringList("roles")
        );
    }
}
```

Spring resolves `@AuthenticationPrincipal Jwt` from:

```text
SecurityContextHolder
    ↓
JwtAuthenticationToken
    ↓
Jwt principal
```

No manual token parsing is required in the controller.

---

## 38. Accessing Authentication in Application Code

### Inject `Jwt`

```java
@GetMapping("/me")
CurrentUserResponse currentUser(
        @AuthenticationPrincipal Jwt jwt
) {
    ...
}
```

### Inject `JwtAuthenticationToken`

```java
@GetMapping("/me")
CurrentUserResponse currentUser(
        JwtAuthenticationToken authentication
) {
    Jwt jwt = authentication.getToken();
    ...
}
```

### Inject generic `Authentication`

```java
@GetMapping("/me")
CurrentUserResponse currentUser(
        Authentication authentication
) {
    String name = authentication.getName();
    Collection<? extends GrantedAuthority> authorities =
            authentication.getAuthorities();
    ...
}
```

### Read SecurityContextHolder directly

```java
Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();
```

Prefer method-parameter injection or an application-specific current-user abstraction instead of repeatedly reading `SecurityContextHolder` throughout business code.

---

## 39. Complete Sequence Diagram

```mermaid
sequenceDiagram
    actor Client
    participant Container as Tomcat
    participant Chain as SecurityFilterChain
    participant Bearer as BearerTokenAuthenticationFilter
    participant Manager as AuthenticationManager
    participant Provider as JwtAuthenticationProvider
    participant Decoder as JwtDecoder
    participant Validator as JWT Validators
    participant Converter as JwtAuthenticationConverter
    participant Context as SecurityContextHolder
    participant Authz as AuthorizationFilter
    participant Controller

    Client->>Container: GET protected API + Bearer JWT
    Container->>Chain: HttpServletRequest
    Chain->>Bearer: Execute filter

    Bearer->>Bearer: Resolve Authorization header
    Bearer->>Bearer: Create BearerTokenAuthenticationToken
    Bearer->>Manager: authenticate(authentication request)

    Manager->>Provider: authenticate(bearer token)
    Provider->>Decoder: decode(raw JWT)

    Decoder->>Decoder: Parse header.payload.signature
    Decoder->>Decoder: Enforce RS256
    Decoder->>Decoder: Verify RSA signature
    Decoder->>Validator: Validate issuer and timestamps

    alt JWT invalid
        Validator-->>Provider: JwtException
        Provider-->>Manager: AuthenticationException
        Manager-->>Bearer: Authentication failure
        Bearer-->>Client: 401 Unauthorized
    else JWT valid
        Validator-->>Decoder: Valid
        Decoder-->>Provider: Jwt
        Provider->>Converter: convert(Jwt)
        Converter->>Converter: roles to ROLE_* authorities
        Converter-->>Provider: JwtAuthenticationToken
        Provider-->>Manager: Authenticated Authentication
        Manager-->>Bearer: Authenticated Authentication
        Bearer->>Context: Store Authentication
        Bearer->>Authz: Continue filter chain

        alt Authorization denied
            Authz-->>Client: 403 Forbidden
        else Authorization granted
            Authz->>Controller: Invoke protected endpoint
            Controller-->>Client: API response
        end
    end
```

---

## 40. Successful Request Example

Request:

```http
GET /api/v1/users/me
Authorization: Bearer <valid-jwt>
```

Processing:

```text
Bearer token found
    ↓
Token structurally valid
    ↓
RS256 algorithm accepted
    ↓
RSA signature verified
    ↓
Issuer accepted
    ↓
Token not expired
    ↓
Roles mapped to authorities
    ↓
JwtAuthenticationToken created
    ↓
SecurityContext populated
    ↓
authenticated() succeeds
    ↓
Controller executes
```

Response:

```http
HTTP/1.1 200 OK
```

---

## 41. Missing Token Failure

```text
No Authorization header
    ↓
No bearer authentication
    ↓
Protected endpoint requires authenticated user
    ↓
AuthenticationEntryPoint
    ↓
401 Unauthorized
```

---

## 42. Malformed Token Failure

Request:

```http
Authorization: Bearer abc123
```

Processing:

```text
Token cannot be parsed as JWT
    ↓
JwtDecoder rejects token
    ↓
Authentication fails
    ↓
401 Unauthorized
```

---

## 43. Modified Payload Failure

Original:

```json
{
  "roles": ["USER"]
}
```

Modified:

```json
{
  "roles": ["ADMIN"]
}
```

Processing:

```text
Payload changed
    ↓
Signature no longer matches
    ↓
RSA verification fails
    ↓
401 Unauthorized
```

---

## 44. Expired Token Failure

```text
Signature valid
    ↓
Issuer valid
    ↓
Current time >= exp
    ↓
Timestamp validation fails
    ↓
401 Unauthorized
```

A valid signature alone is insufficient. Every configured validator must pass.

---

## 45. Wrong Issuer Failure

```text
Signature may be valid
    ↓
iss != configured OdinSync issuer
    ↓
Issuer validation fails
    ↓
401 Unauthorized
```

---

## 46. Valid JWT but Missing Role

Token:

```json
{
  "roles": ["USER"]
}
```

Rule:

```java
.hasRole("ADMIN")
```

Processing:

```text
Authentication succeeds
    ↓
Authorities = ROLE_USER
    ↓
ROLE_ADMIN absent
    ↓
AccessDeniedHandler
    ↓
403 Forbidden
```

---

## 47. AuthenticationEntryPoint

Authentication failures occurring in the security filter chain are not normally handled by `@RestControllerAdvice`.

OdinSync uses:

```java
OdinSyncAuthenticationEntryPoint
```

Example:

```java
@Component
public class OdinSyncAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(
                        "UNAUTHORIZED",
                        "Authentication is required"
                )
        );
    }
}
```

Use it for:

- missing authentication;
- malformed token;
- invalid signature;
- expired token;
- invalid issuer.

Do not expose internal cryptographic exception details.

---

## 48. AccessDeniedHandler

Authorization failures use:

```java
OdinSyncAccessDeniedHandler
```

Example:

```java
@Component
public class OdinSyncAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(
                        "ACCESS_DENIED",
                        "You do not have permission "
                                + "to access this resource"
                )
        );
    }
}
```

---

## 49. Does OdinSync Query the Database?

During normal JWT validation:

```text
No.
```

Spring validates:

- token structure;
- algorithm;
- signature;
- issuer;
- expiration;
- not-before timestamp;
- claims;
- authorities.

It does not normally call:

```text
OdinSyncUserDetailsService
UserJpaRepository
TenantJpaRepository
RoleJpaRepository
```

This is what makes protected requests stateless and fast.

---

## 50. Consequence of No Database Check

Suppose a user logs in with:

```json
{
  "roles": ["ADMIN"]
}
```

Later, the database role is revoked.

The existing JWT still contains:

```json
{
  "roles": ["ADMIN"]
}
```

Until the token expires, its signed claims remain valid under the current model.

Similar scenarios:

- user is disabled;
- tenant is suspended;
- permissions are changed;
- role is revoked.

Mitigations planned for later phases:

- short-lived access tokens;
- refresh-token validation;
- authorization-version claim;
- user/tenant token version;
- Redis-backed version or revocation check;
- denylist for exceptional incidents;
- database checks for especially sensitive operations.

OdinSync currently uses a short-lived access-token strategy. Immediate authorization revocation will be designed after refresh-token implementation.

---

## 51. Custom Filter Comparison

### Custom filter flow

```text
Custom JwtAuthenticationFilter
    ↓
Read header manually
    ↓
Parse JWT manually
    ↓
Load user from database
    ↓
Create UsernamePasswordAuthenticationToken
    ↓
Set SecurityContextHolder manually
```

### Resource-server flow

```text
BearerTokenAuthenticationFilter
    ↓
AuthenticationManager
    ↓
JwtAuthenticationProvider
    ↓
JwtDecoder
    ↓
JwtAuthenticationConverter
    ↓
JwtAuthenticationToken
    ↓
SecurityContextHolder populated by Spring
```

The resource-server approach:

- uses standard Spring Security components;
- avoids hand-written parsing;
- uses provider-based authentication;
- gives consistent errors;
- avoids a database query on every request;
- scales better across microservices;
- supports public-key verification.

Do not configure both a custom JWT filter and OAuth2 Resource Server for the same access token.

---

## 52. Why the Login AuthenticationManager Still Exists

OdinSync publishes an `AuthenticationManager` for login:

```java
@Bean
AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
) throws Exception {
    return configuration.getAuthenticationManager();
}
```

The login adapter explicitly calls it using:

```java
UsernamePasswordAuthenticationToken
```

The selected provider is:

```text
DaoAuthenticationProvider
```

The resource-server configuration separately wires bearer-token authentication using:

```text
JwtAuthenticationProvider
```

Mental model:

```text
Authentication mechanisms
├── Username/password login
│   ├── UsernamePasswordAuthenticationToken
│   ├── DaoAuthenticationProvider
│   ├── UserDetailsService
│   └── PasswordEncoder
│
└── Protected JWT request
    ├── BearerTokenAuthenticationToken
    ├── JwtAuthenticationProvider
    ├── JwtDecoder
    └── JwtAuthenticationConverter
```

---

## 53. Request Authorization Rules

Examples:

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(
                HttpMethod.POST,
                "/api/v1/auth/register",
                "/api/v1/auth/login"
        )
        .permitAll()

        .requestMatchers(
                HttpMethod.GET,
                "/api/v1/users/me"
        )
        .authenticated()

        .requestMatchers("/api/v1/admin/**")
        .hasRole("ADMIN")

        .anyRequest()
        .authenticated()
)
```

Matcher order matters. More specific rules should appear before broad rules such as:

```java
.anyRequest().authenticated()
```

---

## 54. Method-Level Authorization

After enabling:

```java
@EnableMethodSecurity
```

OdinSync can use:

```java
@PreAuthorize("hasRole('OWNER')")
```

Example:

```java
@PreAuthorize("hasRole('OWNER')")
public void updateOrganizationSettings(...) {
}
```

Or permission-based checks:

```java
@PreAuthorize(
        "hasAuthority('organization.settings.update')"
)
```

Method authorization uses the same `Authentication` stored in `SecurityContextHolder`.

Fine-grained RBAC is documented separately.

---

## 55. Testing Checklist

### Valid JWT

Expected:

```text
200 OK
```

### Missing JWT

Expected:

```text
401 Unauthorized
```

### Invalid token format

Expected:

```text
401 Unauthorized
```

### Modified token

Expected:

```text
401 Unauthorized
```

### Expired token

Expected:

```text
401 Unauthorized
```

### Incorrect issuer

Expected:

```text
401 Unauthorized
```

### Valid JWT without required role

Expected:

```text
403 Forbidden
```

### Role claim conversion

Input:

```json
{
  "roles": ["OWNER"]
}
```

Expected authority:

```text
ROLE_OWNER
```

### SecurityContext

Expected authentication:

```text
Type: JwtAuthenticationToken
Authenticated: true
Principal: Jwt
Name: sub claim
Authorities: ROLE_*
```

---

## 56. Manual Verification

### Login

```bash
curl -s \
  -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@odinsync.com",
    "password": "Password@123"
  }'
```

### Valid protected request

```bash
curl -i \
  http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

Expected:

```http
HTTP/1.1 200 OK
```

### Missing token

```bash
curl -i \
  http://localhost:8080/api/v1/users/me
```

Expected:

```http
HTTP/1.1 401 Unauthorized
```

### Invalid token

```bash
curl -i \
  http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer invalid-token"
```

Expected:

```http
HTTP/1.1 401 Unauthorized
```

---

## 57. Debugging Scenarios

### Every request returns 401

Check:

1. Is `spring-boot-starter-oauth2-resource-server` present?
2. Is `.oauth2ResourceServer(...jwt...)` configured?
3. Is a `JwtDecoder` bean available?
4. Is the expected public key loaded?
5. Does the token use RS256?
6. Does the issuer match?
7. Is the token expired?
8. Did local development keys change after restart?
9. Is the header exactly `Authorization: Bearer <token>`?

### Authentication works but roles are empty

Check:

1. Does the JWT contain a `roles` claim?
2. Is it an array of strings?
3. Is the converter reading the correct claim name?
4. Does it add `ROLE_` consistently?
5. Is the custom converter registered in the resource-server DSL?

### `hasRole("OWNER")` returns 403

Check whether authorities contain:

```text
ROLE_OWNER
```

`hasRole("OWNER")` does not check for plain `OWNER`.

### Old token stops working after local restart

Development mode generated a new RSA key pair. The old token was signed with the previous private key and cannot be verified by the new public key.

This is expected for ephemeral development keys.

### Controller receives no `Jwt`

Check:

- endpoint was reached;
- method parameter uses `@AuthenticationPrincipal Jwt`;
- authentication type is `JwtAuthenticationToken`;
- no custom filter replaced the resource-server authentication.

---

## 58. Production Best Practices

- Use HTTPS for all token-bearing traffic.
- Keep access-token TTL short.
- Store private signing keys in a secret manager or mounted secret.
- Never commit private keys.
- Use persistent keys in development and production environments.
- Plan key rotation using key IDs (`kid`) and multiple verification keys.
- Validate issuer.
- Consider audience validation for multiple APIs.
- Never put passwords or sensitive secrets in JWT claims.
- Log token metadata only when necessary; never log full bearer tokens.
- Use structured 401 and 403 responses without cryptographic details.
- Keep server clocks synchronized.
- Use short-lived access tokens plus refresh-token rotation.
- Add authorization-version validation when immediate permission revocation is required.
- Test malformed, expired, modified and wrong-issuer tokens.
- Avoid combining a custom JWT filter with resource-server JWT authentication.

---

## 59. Complete Mental Model

```text
Client has a signed access token
        ↓
Client sends Bearer token
        ↓
Spring extracts it
        ↓
Spring wraps it as an unauthenticated request
        ↓
AuthenticationManager selects JwtAuthenticationProvider
        ↓
JwtDecoder verifies the signature and claims
        ↓
JwtAuthenticationConverter maps roles
        ↓
Spring creates JwtAuthenticationToken
        ↓
Spring stores it in SecurityContextHolder
        ↓
AuthorizationFilter evaluates endpoint rules
        ↓
Controller executes only when access is granted
```

The most important distinction is:

```text
JwtDecoder
    proves that the token is genuine and currently valid.

JwtAuthenticationConverter
    translates trusted claims into Spring authorities.

AuthorizationManager
    decides whether those authorities may access the endpoint.
```

---

## 60. References

This implementation follows Spring Security's standard servlet OAuth2 Resource Server architecture:

- OAuth2 Resource Server JWT authentication
- Servlet authentication architecture
- `SecurityContextHolder`
- `ProviderManager`
- `AuthorizationFilter`
- `authorizeHttpRequests`
- method security

The resource-server DSL automatically wires the bearer-token filter and JWT authentication provider when JWT support is configured.
