# JWT Key Configuration and Token Lifecycle

## 1. Purpose

This document explains how `JwtKeyConfiguration` prepares the cryptographic components used by OdinSync to:

- sign JWT access tokens;
- verify JWT access tokens;
- load RSA keys from external resources;
- generate temporary development keys;
- validate token issuer, signature, and expiration.

The configuration creates three Spring beans:

```text
KeyPair
JwtEncoder
JwtDecoder
```

These beans are created once when the Spring Boot application starts.

---

## 2. High-Level Responsibility

`JwtKeyConfiguration` does not authenticate usernames or passwords.

Its responsibility begins after credential authentication succeeds.

```text
Username/password authentication
        ↓
AuthenticatedUser
        ↓
JwtEncoder signs access token
        ↓
Client receives JWT
        ↓
Client calls protected API
        ↓
JwtDecoder verifies JWT
```

---

## 3. Source Configuration

```java
@Configuration
@EnableConfigurationProperties(OdinSyncJwtProperties.class)
class JwtKeyConfiguration {

    @Bean
    KeyPair jwtKeyPair(
            OdinSyncJwtProperties properties,
            ResourceLoader resourceLoader
    ) {
        if (properties.generateDevelopmentKeys()) {
            return generateDevelopmentKeyPair();
        }

        if (isBlank(properties.privateKeyLocation())
                || isBlank(properties.publicKeyLocation())) {
            throw new IllegalStateException(
                    "JWT RSA key locations must be configured "
                            + "or development key generation must be enabled."
            );
        }

        return new KeyPair(
                readPublicKey(
                        properties.publicKeyLocation(),
                        resourceLoader
                ),
                readPrivateKey(
                        properties.privateKeyLocation(),
                        resourceLoader
                )
        );
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        RSAPublicKey publicKey =
                (RSAPublicKey) jwtKeyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) jwtKeyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(
                        new JWKSet(rsaKey)
                )
        );
    }

    @Bean
    JwtDecoder jwtDecoder(
            KeyPair jwtKeyPair,
            OdinSyncJwtProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(
                        (RSAPublicKey) jwtKeyPair.getPublic()
                )
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer()
                )
        );

        return decoder;
    }
}
```

---

## 4. Application Startup Workflow

```mermaid
flowchart TD
    A[Spring Boot starts] --> B[Load OdinSyncJwtProperties]
    B --> C{Generate development keys?}
    C -->|Yes| D[Generate temporary RSA KeyPair]
    C -->|No| E[Validate public and private key locations]
    E --> F[Load public key PEM]
    E --> G[Load private key PEM]
    F --> H[Create KeyPair bean]
    G --> H
    D --> H
    H --> I[Create JwtEncoder bean]
    H --> J[Create JwtDecoder bean]
    I --> K[Application can issue JWTs]
    J --> L[Application can validate JWTs]
```

The bean dependency graph is:

```text
OdinSyncJwtProperties
        ↓
KeyPair bean
   ├───────────────┐
   ↓               ↓
JwtEncoder      JwtDecoder
   ↓               ↓
Token creation  Token validation
```

---

## 5. `jwtKeyPair()` Bean

```java
@Bean
KeyPair jwtKeyPair(
        OdinSyncJwtProperties properties,
        ResourceLoader resourceLoader
)
```

This method decides where OdinSync obtains its RSA keys.

There are two supported modes:

1. generate temporary development keys;
2. load persistent keys from configured resources.

### 5.1 Development Mode

```java
if (properties.generateDevelopmentKeys()) {
    return generateDevelopmentKeyPair();
}
```

Example local configuration:

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
      access-token-ttl: 15m
      generate-development-keys: true
```

Startup behaviour:

```text
Application starts
        ↓
RSA KeyPair generated in memory
        ↓
All access tokens use this private key
        ↓
All protected requests use this public key
```

After an application restart:

```text
Old KeyPair is lost
        ↓
New KeyPair is generated
        ↓
Previously issued development tokens become invalid
```

This is acceptable for local development but unsuitable for production.

### 5.2 External Key Mode

When development-key generation is disabled, both key locations are required:

```java
if (isBlank(properties.privateKeyLocation())
        || isBlank(properties.publicKeyLocation())) {
    throw new IllegalStateException(...);
}
```

Example configuration:

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
      access-token-ttl: 15m
      generate-development-keys: false
      private-key-location: ${ODINSYNC_JWT_PRIVATE_KEY_LOCATION}
      public-key-location: ${ODINSYNC_JWT_PUBLIC_KEY_LOCATION}
```

Example environment variables:

```bash
ODINSYNC_JWT_PRIVATE_KEY_LOCATION=file:/run/secrets/jwt-private.pem
ODINSYNC_JWT_PUBLIC_KEY_LOCATION=file:/run/secrets/jwt-public.pem
```

The `KeyPair` constructor expects this order:

```java
new KeyPair(publicKey, privateKey)
```

---

## 6. Why RSA Uses Two Keys

RS256 uses asymmetric cryptography.

```text
Private key
    signs tokens

Public key
    verifies tokens
```

The private key must remain secret.

The public key can be shared with services that need to validate OdinSync tokens.

Future microservice model:

```mermaid
flowchart LR
    A[Identity Service] -->|Signs JWT with private key| B[Client]
    B -->|Bearer JWT| C[Order Service]
    B -->|Bearer JWT| D[Inventory Service]
    B -->|Bearer JWT| E[Finance Service]
    C -->|Verify with public key| F[Accepted or rejected]
    D -->|Verify with public key| F
    E -->|Verify with public key| F
```

Only the identity component needs the private key.

Other services only need the public key.

---

## 7. `generateDevelopmentKeyPair()`

```java
private static KeyPair generateDevelopmentKeyPair() {
    try {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    } catch (GeneralSecurityException exception) {
        throw new IllegalStateException(
                "Unable to generate development JWT key pair",
                exception
        );
    }
}
```

### Execution steps

```text
Request RSA generator
        ↓
Set key size to 2048 bits
        ↓
Generate public key
        ↓
Generate private key
        ↓
Return KeyPair
```

The key pair is generated once during application startup because Spring beans are singleton-scoped by default.

It is not generated for every login request.

```text
Application startup    → one KeyPair
Login request 1        → same private key
Login request 2        → same private key
Protected request      → same public key
Application restart    → new development KeyPair
```

---

## 8. Reading a Private Key

Expected private-key format:

```text
-----BEGIN PRIVATE KEY-----
Base64 encoded PKCS#8 key
-----END PRIVATE KEY-----
```

Implementation:

```java
private static RSAPrivateKey readPrivateKey(
        String location,
        ResourceLoader resourceLoader
) {
    try {
        String pem = readPem(location, resourceLoader)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] decoded = Base64
                .getMimeDecoder()
                .decode(pem);

        return (RSAPrivateKey) KeyFactory
                .getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(decoded)
                );

    } catch (GeneralSecurityException exception) {
        throw new IllegalStateException(
                "Unable to read JWT private key",
                exception
        );
    }
}
```

Conversion workflow:

```text
PEM text
        ↓
Remove BEGIN/END markers
        ↓
Base64 decode
        ↓
PKCS8EncodedKeySpec
        ↓
RSA KeyFactory
        ↓
RSAPrivateKey
```

`PKCS8EncodedKeySpec` is used because the private key is expected in PKCS#8 format.

---

## 9. Reading a Public Key

Expected public-key format:

```text
-----BEGIN PUBLIC KEY-----
Base64 encoded X.509 key
-----END PUBLIC KEY-----
```

Implementation:

```java
private static RSAPublicKey readPublicKey(
        String location,
        ResourceLoader resourceLoader
) {
    try {
        String pem = readPem(location, resourceLoader)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] decoded = Base64
                .getMimeDecoder()
                .decode(pem);

        return (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(
                        new X509EncodedKeySpec(decoded)
                );

    } catch (GeneralSecurityException exception) {
        throw new IllegalStateException(
                "Unable to read JWT public key",
                exception
        );
    }
}
```

Conversion workflow:

```text
PEM text
        ↓
Remove BEGIN/END markers
        ↓
Base64 decode
        ↓
X509EncodedKeySpec
        ↓
RSA KeyFactory
        ↓
RSAPublicKey
```

Key-format distinction:

```text
Private key → PKCS8EncodedKeySpec
Public key  → X509EncodedKeySpec
```

---

## 10. `ResourceLoader`

`ResourceLoader` allows the application to load keys from different resource locations.

```java
Resource resource = resourceLoader.getResource(location);
```

Supported examples:

```text
classpath:keys/jwt-public.pem
file:/run/secrets/jwt-public.pem
file:/opt/odinsync/keys/jwt-public.pem
```

The content is read using UTF-8:

```java
resource.getContentAsString(StandardCharsets.UTF_8);
```

For production, prefer mounted secrets or a secret-management integration rather than embedding private keys inside the application JAR.

---

## 11. `jwtEncoder()` Bean

```java
@Bean
JwtEncoder jwtEncoder(KeyPair jwtKeyPair)
```

The encoder signs JWT access tokens.

It uses both RSA keys to construct a Nimbus `RSAKey`:

```java
RSAPublicKey publicKey =
        (RSAPublicKey) jwtKeyPair.getPublic();

RSAPrivateKey privateKey =
        (RSAPrivateKey) jwtKeyPair.getPrivate();

RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .build();
```

The private key performs the signature operation.

The public key supplies the public RSA parameters and supports standardized JWK representation.

The encoder is created from a JWK set:

```java
return new NimbusJwtEncoder(
        new ImmutableJWKSet<SecurityContext>(
                new JWKSet(rsaKey)
        )
);
```

### JWK terminology

```text
JWK    = JSON Web Key
JWKSet = collection of JSON Web Keys
```

Currently OdinSync has one signing key.

```text
JWKSet
└── Current RSA signing key
```

A future key-rotation design may contain:

```text
JWKSet
├── Current signing key
└── Previous verification key
```

---

## 12. How `JwtEncoder` Is Used

After username/password authentication and account-status checks succeed:

```java
GeneratedAccessToken generatedToken =
        accessTokenGenerator.generate(authenticatedUser);
```

Inside the token generator:

```java
JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .subject(authenticatedUser.userId().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .id(UUID.randomUUID().toString())
        .claim(
                "tenant_id",
                authenticatedUser.tenantId().toString()
        )
        .claim("email", authenticatedUser.email())
        .claim("roles", authenticatedUser.roles())
        .build();
```

The token is encoded and signed:

```java
String token = jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)
).getTokenValue();
```

Signing workflow:

```mermaid
flowchart TD
    A[AuthenticatedUser] --> B[Build JWT claims]
    B --> C[Build RS256 header]
    C --> D[JwtEncoder]
    D --> E[Sign using RSA private key]
    E --> F[Return signed JWT]
```

JWT structure:

```text
header.payload.signature
```

The signature protects the header and payload against modification.

---

## 13. `jwtDecoder()` Bean

```java
@Bean
JwtDecoder jwtDecoder(
        KeyPair jwtKeyPair,
        OdinSyncJwtProperties properties
)
```

The decoder validates JWTs sent to protected endpoints.

```java
NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withPublicKey(
                (RSAPublicKey) jwtKeyPair.getPublic()
        )
        .signatureAlgorithm(SignatureAlgorithm.RS256)
        .build();
```

The decoder uses only the public key.

The private key is not required for validation.

### Enforced algorithm

```java
.signatureAlgorithm(SignatureAlgorithm.RS256)
```

This means OdinSync accepts tokens signed using:

```text
RSA signature + SHA-256
```

A token using an unexpected signing algorithm is rejected.

---

## 14. Issuer and Time Validation

```java
decoder.setJwtValidator(
        JwtValidators.createDefaultWithIssuer(
                properties.issuer()
        )
);
```

This validates important claims, including:

```text
iss → expected token issuer
exp → expiration time
nbf → not-before time, when present
```

Example configured issuer:

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
```

Expected token claim:

```json
{
  "iss": "odinsync-platform"
}
```

A token issued by another system is rejected even when it is otherwise well formed.

---

## 15. Protected Request Validation Flow

Spring Security's OAuth2 Resource Server uses the `JwtDecoder` bean automatically.

Example request:

```http
GET /api/v1/customers
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

Runtime workflow:

```mermaid
sequenceDiagram
    actor Client
    participant Filter as BearerTokenAuthenticationFilter
    participant Decoder as JwtDecoder
    participant Validator as JWT Validators
    participant Converter as JwtAuthenticationConverter
    participant Context as SecurityContext
    participant Controller

    Client->>Filter: Request with Bearer JWT
    Filter->>Decoder: decode(token)
    Decoder->>Decoder: Verify RSA signature with public key
    Decoder->>Validator: Validate issuer and timestamps

    alt Token invalid
        Decoder-->>Filter: JwtException
        Filter-->>Client: 401 Unauthorized
    else Token valid
        Decoder-->>Filter: Decoded Jwt
        Filter->>Converter: Convert claims to authorities
        Converter-->>Filter: Authentication
        Filter->>Context: Store Authentication
        Filter->>Controller: Continue request
        Controller-->>Client: Protected response
    end
```

Spring handles this automatically when the resource server is configured:

```java
.oauth2ResourceServer(resourceServer ->
        resourceServer.jwt(jwt -> {
        })
)
```

---

## 16. Signing Versus Verification

```text
JwtEncoder
    uses private key
    signs new JWTs

JwtDecoder
    uses public key
    verifies existing JWTs
```

### Login lifecycle

```text
Credentials validated
        ↓
User and tenant active
        ↓
JwtEncoder signs claims
        ↓
JWT returned to client
```

### Protected-request lifecycle

```text
Client sends JWT
        ↓
JwtDecoder verifies signature
        ↓
Issuer and time claims validated
        ↓
Authentication created
        ↓
Request continues
```

---

## 17. Complete End-to-End Workflow

```mermaid
flowchart TD
    A[Application startup] --> B[Create or load RSA KeyPair]
    B --> C[Create JwtEncoder]
    B --> D[Create JwtDecoder]

    E[User login] --> F[Validate email and password]
    F --> G[Create AuthenticatedUser]
    G --> H[Build JWT claims]
    H --> C
    C --> I[Sign JWT with private key]
    I --> J[Return access token]

    J --> K[Client calls protected API]
    K --> L[BearerTokenAuthenticationFilter]
    L --> D
    D --> M[Verify signature with public key]
    M --> N[Validate issuer and expiry]
    N --> O[Create Authentication]
    O --> P[Store in SecurityContext]
    P --> Q[Execute controller]
```

---

## 18. Security Rules

### Private key

The private key:

- must never be committed to Git;
- must never be logged;
- must not be distributed to business microservices;
- should be stored in a secret-management system;
- should be readable only by the token-issuing service.

### Public key

The public key:

- can be shared with token-consuming services;
- cannot be used to issue valid tokens;
- is used only to verify signatures.

### Development keys

Generated development keys:

- are temporary;
- exist only in memory;
- invalidate old JWTs after restart;
- must not be enabled in production.

---

## 19. Configuration Examples

### Local profile

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
      access-token-ttl: 15m
      generate-development-keys: true
      private-key-location:
      public-key-location:
```

### Development or production profile

```yaml
odinsync:
  security:
    jwt:
      issuer: odinsync-platform
      access-token-ttl: 15m
      generate-development-keys: false
      private-key-location: ${ODINSYNC_JWT_PRIVATE_KEY_LOCATION}
      public-key-location: ${ODINSYNC_JWT_PUBLIC_KEY_LOCATION}
```

---

## 20. Common Failure Scenarios

### Missing key locations

```text
generate-development-keys = false
private key location missing
or public key location missing
        ↓
Application startup fails
```

Expected error:

```text
JWT RSA key locations must be configured or development key generation must be enabled.
```

### Invalid private-key format

```text
Private key is not PKCS#8
        ↓
PKCS8EncodedKeySpec cannot parse it
        ↓
Application startup fails
```

### Invalid public-key format

```text
Public key is not X.509 SubjectPublicKeyInfo format
        ↓
X509EncodedKeySpec cannot parse it
        ↓
Application startup fails
```

### Token signed by another key

```text
JWT signature generated with unknown private key
        ↓
Configured public key cannot verify signature
        ↓
401 Unauthorized
```

### Expired token

```text
Current time is after exp
        ↓
JWT validation fails
        ↓
401 Unauthorized
```

### Wrong issuer

```text
iss does not equal configured issuer
        ↓
JWT validation fails
        ↓
401 Unauthorized
```

### Application restart in local mode

```text
Old development key removed
        ↓
New KeyPair generated
        ↓
Existing local tokens fail signature validation
```

---

## 21. Debugging Checklist

When JWT creation or validation fails, verify:

1. Is the expected Spring profile active?
2. Is `generate-development-keys` correct for that environment?
3. Are both external key locations configured when generation is disabled?
4. Does `ResourceLoader` resolve the configured locations?
5. Is the private key PKCS#8 with `BEGIN PRIVATE KEY` markers?
6. Is the public key X.509 with `BEGIN PUBLIC KEY` markers?
7. Are the public and private keys from the same RSA pair?
8. Is the token signed with RS256?
9. Does the `iss` claim match the configured issuer?
10. Is the token expired?
11. Was the local application restarted after the token was issued?
12. Is the bearer token sent as `Authorization: Bearer <token>`?

---

## 22. Mental Model

Think of the RSA private key as OdinSync's protected company stamp.

```text
Private key
= creates the official signature
```

Think of the public key as the signature-verification template.

```text
Public key
= verifies that the signature is genuine
```

Anyone with the public key can verify an OdinSync token.

Only the component with the private key can issue a valid OdinSync token.

---

## 23. Summary

`JwtKeyConfiguration` performs the following work:

```text
Application startup
        ↓
Generate temporary keys or load persistent keys
        ↓
Expose KeyPair as a Spring bean
        ↓
Create JwtEncoder with private signing capability
        ↓
Create JwtDecoder with public verification capability
```

During login:

```text
AuthenticatedUser
        ↓
JwtEncoder
        ↓
Private key signs claims
        ↓
Access token returned
```

During a protected request:

```text
Bearer token
        ↓
JwtDecoder
        ↓
Public key verifies signature
        ↓
Issuer and timestamps validated
        ↓
Authentication stored in SecurityContext
```

This separation allows OdinSync to issue tokens securely today and later share public verification keys across independently deployed microservices.
