# Organization Implementation Baseline

## Assessment Date

2026-07-20

## Repository Baseline

- Repository root: `odinsync-platform`.
- Backend location: `backend/`.
- Maven structure: single Maven project under `backend/`; no root `pom.xml` was found.
- Maven Wrapper: `backend/mvnw`.
- Application entry point: `backend/src/main/java/com/odinsync/OdinsyncPlatformApplication.java`.
- Base package: `com.odinsync`.
- Java version: `21` via `java.version` and `maven.compiler.release`.
- Spring Boot version: `3.5.16`.
- Source root: `backend/src/main/java`.
- Test root: `backend/src/test/java`.
- Resource root: `backend/src/main/resources`.
- Active Maven profiles: none active by default.
- Explicit Maven test plugin: `maven-surefire-plugin`.
- Integration-test plugin: none configured.
- Formatting, static-analysis, coverage, architecture-test, and Testcontainers dependencies: none configured.
- Established test command: `cd backend && ./mvnw test`.

## Current Build Result

Baseline command run before this document was added:

```text
cd backend && ./mvnw test
```

Result:

- Build status: success.
- Tests run: 69.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- Relevant warnings: Maven/Guice uses a terminally deprecated `sun.misc.Unsafe` method; Mockito warns that self-attaching for inline mocking will not work in future JDK releases.

`cd backend && ./mvnw help:active-profiles` initially failed in the sandbox because Maven could not write resolver metadata under `~/.m2`; rerun with approval succeeded and showed no active profiles.

Post-documentation test result:

- Build status: success.
- Tests run: 69.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- Relevant warnings: same Maven/Guice `sun.misc.Unsafe` deprecation warning and Mockito self-attach warning as the baseline run.

## Existing Organization Implementation

Organization code currently exists only in package-boundary placeholders under `backend/src/main/java/com/odinsync/organization/*/package-info.java`.

The implemented organization registration behavior is currently owned by the Identity vertical slice:

- Use case: `backend/src/main/java/com/odinsync/identity/application/usecase/RegisterOrganizationUseCase.java`.
- Input port: `backend/src/main/java/com/odinsync/identity/application/port/in/RegisterOrganizationPort.java`.
- Output port: `backend/src/main/java/com/odinsync/identity/application/port/out/OrganizationRepositoryPort.java`.
- Identity domain record: `backend/src/main/java/com/odinsync/identity/domain/model/Organization.java`.
- JPA entity: `backend/src/main/java/com/odinsync/identity/infrastructure/persistence/entity/OrganizationJpaEntity.java`.
- Persistence adapter: `backend/src/main/java/com/odinsync/identity/infrastructure/persistence/adapter/OrganizationPersistenceAdapter.java`.
- Spring Data repository: `backend/src/main/java/com/odinsync/identity/infrastructure/persistence/repository/OrganizationJpaRepository.java`.
- API: `POST /api/v1/auth/register` in `backend/src/main/java/com/odinsync/identity/presentation/controller/RegisterOrganizationController.java`.

Registration creates tenant, organization, owner user, OWNER role, and user-role assignment inside one `@Transactional` Identity use case. Future Organization phases must preserve this bootstrap flow until ownership is deliberately migrated.

## Reusable Shared Abstractions

- Shared UTC clock bean exists at `backend/src/main/java/com/odinsync/shared/config/TimeConfiguration.java`.
- Shared error response exists at `backend/src/main/java/com/odinsync/shared/exception/ApiErrorResponse.java`.
- Shared global exception handler exists at `backend/src/main/java/com/odinsync/shared/exception/GlobalExceptionHandler.java`.
- Shared security configuration exists under `backend/src/main/java/com/odinsync/shared/security`.
- No reusable `TenantId`, `UserId`, `OrganizationId`, `CurrentActorProvider`, `DomainEvent`, `AuditableEntity`, or base JPA entity exists in source.

## Package and Layer Conventions

Bounded contexts use:

```text
com.odinsync.<context>.domain
com.odinsync.<context>.application
com.odinsync.<context>.infrastructure
com.odinsync.<context>.presentation
```

Identity further uses:

```text
application.command
application.model
application.port.in
application.port.out
application.usecase
domain.exception
domain.model
infrastructure.persistence.adapter
infrastructure.persistence.entity
infrastructure.persistence.mapper
infrastructure.persistence.repository
infrastructure.security
presentation.controller
presentation.dto
presentation.rest
```

Organization should follow the same Clean Architecture layout and keep domain models independent of Spring, JPA, web, and Lombok unless a later project-wide standard changes.

## Identifier and UUID Strategy

- Current domain and application models use raw `java.util.UUID`.
- IDs are generated in application/domain code with `UUID.randomUUID()`.
- PostgreSQL columns use native `UUID`.
- JPA entities map IDs as `UUID` with `@Id` and `@Column(nullable = false)`.
- No database default UUID generation or UUID extension is configured.

Organization decision for ORG-01: continue with raw `UUID` to match existing repository reality. Defer value-object IDs until a broader cross-module identifier policy is approved.

## Database and Flyway Baseline

- Database engine: PostgreSQL.
- Driver: `org.postgresql.Driver`.
- Flyway migration location: `backend/src/main/resources/db/migration`.
- Naming convention: `V<version>__<description>.sql`.
- Latest migration: `V3__harden_refresh_token_sessions.sql`.
- Next candidate migration: `V4__...`.
- Current organization table: `organizations`.
- Current organization columns in V1: `id`, `tenant_id`, `name`, `legal_name`, `gst_number`, `email`, `phone`, `address`, `created_at`, `updated_at`.
- Current unique constraint/index: `uk_organizations_tenant_id` on `tenant_id`.
- Current FK: `fk_organizations_tenant` references `tenants(id)`.
- Repeatable migrations: none.
- Migration tests: none.
- UUID extensions/functions: none required by current migrations.
- Existing migrations are checksum-sensitive and must not be edited.

## Auditing and UTC Time Strategy

- Shared `Clock.systemUTC()` bean exists.
- Application services such as refresh-token logic use injected `Clock` for deterministic tests.
- JPA entities use `@PrePersist` and `@PreUpdate`.
- Several JPA callbacks call `Instant.now()` directly, including `OrganizationJpaEntity` and `RefreshTokenJpaEntity`.
- Database migrations also use `DEFAULT CURRENT_TIMESTAMP`.
- No `createdBy` or `updatedBy` convention exists.
- No Spring Data auditing is enabled.

Organization decision: use the shared UTC `Clock` in domain/application code. For persistence, follow existing JPA timestamp callbacks initially, but ORG-01 should not introduce a new audit base class. A later persistence phase should decide whether to standardize entity timestamps on `Clock`.

## Domain Event Strategy

No source-code domain-event mechanism currently exists:

- No `DomainEvent` interface.
- No aggregate event collection convention.
- No `ApplicationEventPublisher` adapter.
- No `@TransactionalEventListener` source implementation.
- No outbox.
- No Kafka.

Organization docs propose domain events and eventual outbox/Kafka, but ORG-00 confirms they are not implemented yet. ORG-02 may design local domain event recording inside the aggregate, but ORG-03 should introduce an event publisher port only when an application use case needs it. ORG-16 should handle publication/outbox decisions explicitly.

## Authentication Principal

Protected requests use Spring Security OAuth2 Resource Server with JWT:

- Security config: `backend/src/main/java/com/odinsync/shared/security/SecurityConfig.java`.
- JWT key config: `backend/src/main/java/com/odinsync/shared/security/JwtKeyConfiguration.java`.
- JWT issuer config: `backend/src/main/java/com/odinsync/shared/security/OdinSyncJwtProperties.java`.
- JWT generation: `backend/src/main/java/com/odinsync/identity/infrastructure/security/JwtAccessTokenGeneratorAdapter.java`.
- JWT conversion: `backend/src/main/java/com/odinsync/identity/infrastructure/security/OdinSyncJwtAuthenticationConverter.java`.
- Principal type received by controllers: `org.springframework.security.oauth2.jwt.Jwt`.
- Authentication token type: `JwtAuthenticationToken`.
- User ID source: JWT `sub`.
- Tenant ID source: JWT claim `tenant_id`.
- Email source: JWT claim `email`.
- Roles source: JWT claim `roles`.
- Permissions in token: not currently present.

Organization endpoints should obtain `userId` and `tenantId` from the authenticated JWT through a dedicated adapter/provider introduced in ORG-03, not from request bodies or headers.

## Current Actor Resolution

No implemented `CurrentActorProvider`, `CurrentUserProvider`, `SecurityContextProvider`, `AuthenticatedPrincipalProvider`, or `TenantContext` exists in source.

Current controllers extract actor data directly:

- `CurrentUserController` uses `@AuthenticationPrincipal Jwt`.
- `RefreshTokenController` reads `jwt.getSubject()` and `jwt.getClaimAsString("tenant_id")`.

Organization decision: introduce an application-facing current actor port in ORG-03 and implement it with a Spring Security JWT adapter in infrastructure/presentation. Do not duplicate JWT parsing inside every Organization use case.

## Authorization Model

Current authorization is role-based:

- JWT `roles` claim contains business role names such as `OWNER`, `ADMIN`, and `MEMBER`.
- `OdinSyncJwtAuthenticationConverter` maps roles to Spring `ROLE_*` authorities.
- `SecurityConfig` uses `hasRole("ADMIN")` and `hasAnyRole("OWNER", "ADMIN")`.
- `SecurityAuthorizationTestController` uses `@PreAuthorize("hasRole('OWNER')")`, `hasRole('ADMIN')`, and `hasAnyRole('OWNER', 'ADMIN')`.
- Permission rows exist in the database, but access tokens do not currently include permissions and the converter does not create permission authorities.

Organization decision: use current role authorities temporarily for protected endpoints. Permission-based Organization authorities are a roadmap dependency because the token model does not yet carry permissions.

## Error Contract

Error response shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "status": 400,
  "timestamp": "2026-07-20T00:00:00Z"
}
```

Actual implementation:

- `ApiErrorResponse` fields: `code`, `message`, `status`, `timestamp`.
- `ApiErrorResponse.of(code, message)` currently sets `status` to `null`.
- `ApiErrorResponse.of(code, message, status)` sets the numeric status.
- `GlobalExceptionHandler` maps validation to `VALIDATION_ERROR`, duplicate email to `EMAIL_ALREADY_EXISTS`, data integrity conflicts to `DATA_INTEGRITY_CONFLICT`, invalid credentials to `INVALID_CREDENTIALS`, invalid refresh token/reuse to `INVALID_REFRESH_TOKEN`, inactive tenant to `INACTIVE_TENANT`, and unexpected errors to `INTERNAL_SERVER_ERROR`.
- Security entry point maps unauthenticated requests to `UNAUTHORIZED`.
- Access-denied handler maps forbidden requests to `ACCESS_DENIED`.
- No `application/problem+json` support exists.
- No trace/correlation ID exists.

Organization should reuse `GlobalExceptionHandler` and add Organization-specific error mappings only through the shared error contract.

## Persistence Conventions

- Domain models and JPA entities are separate.
- Application layer depends on output ports, not Spring Data repositories.
- Infrastructure adapters implement output ports and delegate to Spring Data repositories.
- Mapping is explicit through hand-written mapper classes, not MapStruct or ModelMapper.
- JPA entities use Lombok `@Getter`/`@Setter`.
- Domain records/classes do not use Spring/JPA annotations.
- Enums are persisted as strings in existing entities.
- `RefreshTokenJpaEntity` uses `@Version`; most identity entities do not.
- No standard `@EntityGraph`, `JOIN FETCH`, cascade, orphan-removal, or `@MapsId` convention is established yet.
- Table names are plural snake_case.
- Constraint/index names use prefixes such as `fk_`, `uk_`, and `idx_`.

Organization persistence should follow the port/adapter/repository/mapper split already used by Identity. For mutable Organization aggregate persistence, add optimistic locking only when the schema and use case require concurrent update protection.

## Registration Integration

Current registration transaction in `RegisterOrganizationUseCase`:

1. Normalize and check email uniqueness through `UserRepositoryPort`.
2. Create tenant with status `ACTIVE` and plan `FREE`.
3. Create organization linked to tenant.
4. Hash owner password.
5. Create owner user linked to tenant.
6. Create OWNER role linked to tenant.
7. Assign OWNER role to owner user.
8. Return tenant ID, organization ID, user ID, and message.

Future Organization phases must preserve:

- Atomic tenant/organization/user/role bootstrap.
- Client must not supply `tenant_id`.
- Existing `organizations` rows created by Identity registration.
- Existing `uk_organizations_tenant_id` one-organization-per-tenant assumption.
- Existing API response containing `organizationId`.

Any move from Identity-owned `OrganizationJpaEntity` to Organization-owned persistence will need a deliberate migration/adapter plan rather than a silent duplicate model.

## Testing Strategy

Current test stack:

- JUnit Jupiter through `spring-boot-starter-test`.
- Mockito through Spring Boot test dependencies.
- AssertJ is used in unit tests.
- MockMvc is used for controller/security tests.
- Spring Security Test is present.
- Testcontainers is not present.
- H2 is not present.
- Database integration tests are not present.
- Failsafe integration-test phase is not configured.
- Architecture tests are not present.
- Fixed-clock testing is used in refresh-token unit tests.
- Current test count: 69.

Organization phases should start with focused unit tests using JUnit, Mockito, and AssertJ. Controller/security tests should use MockMvc. Database integration testing requires a later dependency decision because Testcontainers is not currently available.

## Architecture Boundary Risks

| Risk | Impact | Affected phase | Recommended resolution |
| --- | --- | --- | --- |
| `organizations` table and model are currently Identity-owned | Organization module could duplicate or conflict with Identity persistence | ORG-06 to ORG-09 | Decide ownership and introduce a transition adapter/backfill plan before Organization persistence writes |
| PostgreSQL docs and implementation must remain aligned | Wrong SQL types or driver assumptions would break Flyway/JPA validation | ORG-06 | Use PostgreSQL native `UUID` and existing Flyway naming |
| Raw UUID versus value-object mismatch | Introducing ID value objects too early would diverge from Identity conventions | ORG-01 | Use raw `UUID` now; defer value-object IDs to a platform-wide decision |
| Roles-only JWT while docs expect permissions | Permission-based Organization security cannot work from current tokens | ORG-10 to ORG-13 | Use role authorities temporarily or extend token/converter in a dedicated security task |
| Missing `CurrentActorProvider` | Use cases may depend directly on Spring `Jwt` if no adapter is introduced | ORG-03 | Add application port and Spring Security adapter before Organization use cases |
| Missing Testcontainers | Persistence tests against PostgreSQL cannot be added cleanly yet | ORG-07 to ORG-09 | Add Testcontainers only in an approved testing-infrastructure task |
| Missing domain-event abstraction | Aggregate events in docs have no implementation target | ORG-02, ORG-16 | Record events locally first; introduce publisher/outbox later by explicit task |
| Registration transaction coupling | Moving Organization ownership can break tenant onboarding | ORG-06 to ORG-09 | Preserve Identity registration flow until migration and ownership are explicit |
| Existing schema may be thinner than approved Organization design | Future fields/settings may require additive migrations/backfill | ORG-06 | Create additive `V4` migration and avoid editing V1 |
| Cross-module FK coupling to tenants | Later microservice extraction will need boundary decisions | ORG-06 onward | Keep modular monolith references now; document extraction path before service split |

## Confirmed Decisions for ORG-01

- Reuse raw `UUID` for tenant, user, and organization identifiers.
- Add Organization domain primitives/value objects only for Organization-specific concepts, not shared platform identifiers.
- Keep Organization domain free of Spring, JPA, Lombok, and web dependencies.
- Keep package root `com.odinsync.organization`.
- Use Java records/classes consistently with current domain code.
- Defer persistence annotations, migrations, event publishing, authorization, and current actor adapters.

## Deferred Decisions

- Whether Organization should own the existing `organizations` table or consume it through an Identity-owned adapter during transition.
- Whether to introduce shared ID value objects.
- Whether to include permissions in JWT access tokens.
- Whether to add `CurrentActorProvider`.
- Whether to add Testcontainers and database integration tests.
- Whether to introduce a shared domain-event/outbox abstraction.
- Whether to standardize JPA timestamps on injected `Clock`.
- Whether to introduce a shared audit base class.

## Documentation Deviations

- `docs/modules/organization/organization.md` describes future Organization-owned domain, event, actor, and permission patterns that are not implemented yet.
- `docs/modules/organization/implementation_roadmap.md` refers to future `CurrentActorProvider`, `DomainEventPublisher`, and outbox concerns; source code does not currently contain these abstractions.
- Current implementation uses role-based JWT authorities, while Organization docs target permission-based authorization.
- Current Organization persistence exists in Identity, not in the Organization bounded context.

## Decision Table

| Concern | Existing Convention | Organization Decision | Evidence |
| --- | --- | --- | --- |
| Base package | `com.odinsync` | Reuse `com.odinsync.organization` | `backend/src/main/java/com/odinsync/OdinsyncPlatformApplication.java`, `backend/src/main/java/com/odinsync/organization/package-info.java` |
| Layers | `domain`, `application`, `infrastructure`, `presentation` | Reuse | `backend/src/main/java/com/odinsync/identity/*`, `backend/src/main/java/com/odinsync/organization/*/package-info.java` |
| Ports | `application.port.in`, `application.port.out` | Reuse | `backend/src/main/java/com/odinsync/identity/application/port/in`, `backend/src/main/java/com/odinsync/identity/application/port/out` |
| Tenant ID | Raw `UUID` | Reuse raw `UUID` | `backend/src/main/java/com/odinsync/identity/domain/model/Tenant.java` |
| User ID | Raw `UUID` | Reuse raw `UUID` | `backend/src/main/java/com/odinsync/identity/domain/model/User.java` |
| Organization ID | Raw `UUID` | Reuse raw `UUID` | `backend/src/main/java/com/odinsync/identity/domain/model/Organization.java` |
| UUID database type | PostgreSQL `UUID` | Reuse | `backend/src/main/resources/db/migration/V1__create_identity_access_schema.sql` |
| Current actor | Direct `@AuthenticationPrincipal Jwt` | Introduce provider in ORG-03 | `backend/src/main/java/com/odinsync/identity/presentation/rest/CurrentUserController.java` |
| Permissions | Roles in JWT, `ROLE_*` authorities | Use role authorities temporarily | `backend/src/main/java/com/odinsync/identity/infrastructure/security/OdinSyncJwtAuthenticationConverter.java` |
| Error response | `ApiErrorResponse` | Reuse | `backend/src/main/java/com/odinsync/shared/exception/ApiErrorResponse.java` |
| Clock | Shared UTC `Clock` bean | Reuse in application/domain services | `backend/src/main/java/com/odinsync/shared/config/TimeConfiguration.java` |
| Domain events | None in source | Defer implementation; design later | `rg DomainEvent backend/src/main/java` found no source implementation |
| Next migration | Latest is V3 | Reserve V4 candidate | `backend/src/main/resources/db/migration/V3__harden_refresh_token_sessions.sql` |
| Integration tests | No Failsafe/Testcontainers | Use unit/MockMvc now; add DB prerequisite later | `backend/pom.xml`, `backend/src/test/java` |

## Recommended Next Task

ORG-01 Domain Primitives and Value Objects

Phase guidance:

- ORG-01: add only Organization-specific primitives/value objects; reuse raw `UUID`; defer shared ID abstractions.
- ORG-02: aggregate may record local domain events internally, but source has no publisher abstraction to reuse.
- ORG-03: introduce actor, authorization, time, repository, and event ports only if required by use cases; current actor provider is missing.
- ORG-06: likely migration candidate is V4; use additive PostgreSQL migration and plan for existing `organizations` rows.
- ORG-07 to ORG-09: follow Identity persistence with domain/JPA separation, explicit mapper, Spring Data repository, and adapter implementing an output port.
- ORG-10 to ORG-13: follow `/api/v1/...`, record DTOs, Bean Validation, MockMvc tests, `ApiErrorResponse`, and temporary role-based security until permissions are implemented in JWT.
