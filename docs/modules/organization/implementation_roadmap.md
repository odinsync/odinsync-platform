# OdinSync Organization Module Implementation Roadmap

## Objective

Implement the Organization module incrementally rather than through one large Codex task.

Each phase must:

- Have a narrow scope
- Produce working, reviewable code
- Include tests for the implemented behavior
- Pass the existing build before the next phase starts
- Avoid implementing future-phase concerns prematurely

The implementation should follow the completed Organization module documentation and OdinSync’s existing conventions.

---

# Implementation Principles

Before starting any phase:

1. Inspect the current repository.
2. Reuse existing shared abstractions.
3. Confirm package and naming conventions.
4. Check the latest Flyway migration number.
5. Check the existing UUID storage strategy.
6. Check the authenticated-principal model.
7. Check the existing error-contract implementation.
8. Run the current test suite.

Do not create duplicate abstractions for:

- Tenant identifiers
- User identifiers
- Domain events
- Current actor resolution
- Error responses
- Time providers
- Audit metadata
- Permission checks
- Persistence base classes
- Testcontainers configuration
- Test fixtures already available in shared test support

Every phase must end with:

```bash
./mvnw test
```

Where integration tests are configured separately, also run:

```bash
./mvnw verify
```

A phase is not complete until:

- The implementation compiles.
- New tests pass.
- Existing tests remain green.
- No unrelated behavior is changed.
- The phase summary records all deviations from the approved design.

---

# Roadmap Summary

| Phase | Scope | Primary Outcome |
|---|---|---|
| Phase 0 | Repository discovery and baseline | Verified implementation plan based on actual codebase |
| Phase 1 | Shared Organization domain primitives | IDs, enums, value objects, and validation |
| Phase 2 | Organization aggregate | Aggregate behavior, invariants, audit updates, domain events |
| Phase 3 | Application contracts | Repository port, actor port, authorization port, time and event ports |
| Phase 4 | Profile application use cases | Get and update Organization profile |
| Phase 5 | Settings application use cases | Get and update Organization settings |
| Phase 6 | Persistence schema | Flyway migration and schema validation |
| Phase 7 | JPA entity model | Root and owned persistence entities |
| Phase 8 | Persistence mapper | Domain-to-JPA and JPA-to-domain conversion |
| Phase 9 | Repository adapter | Tenant-scoped persistence operations |
| Phase 10 | REST profile APIs | Profile request, response, controller, and validation |
| Phase 11 | REST settings APIs | Settings request, response, controller, and validation |
| Phase 12 | Security integration | Authenticated actor and permission enforcement |
| Phase 13 | Error handling | Stable Organization error catalogue and Problem Details |
| Phase 14 | Tenant-isolation verification | Cross-tenant security and repository tests |
| Phase 15 | Concurrency | Optimistic locking and 409 conflict handling |
| Phase 16 | Event publication | Post-persistence domain event publication |
| Phase 17 | Observability | Structured logs, metrics, and tracing integration |
| Phase 18 | Architecture and end-to-end tests | Boundary enforcement and complete workflows |
| Phase 19 | Documentation reconciliation | Final implementation documentation and readiness review |

---

# Phase 0: Repository Discovery and Baseline

## Goal

Understand the existing OdinSync codebase before adding Organization code.

No Organization business implementation should be added in this phase.

## Tasks

Inspect:

- Root Maven configuration
- Java and Spring Boot versions
- Existing module package structure
- Identity bounded context
- Shared domain abstractions
- Existing security configuration
- Existing JWT principal
- Existing permission representation
- Existing exception hierarchy
- Existing Problem Details response
- Existing Flyway migrations
- Existing MySQL UUID mappings
- Existing auditing strategy
- Existing event publisher abstraction
- Existing Testcontainers setup
- Existing architecture tests
- Existing CI commands

Run:

```bash
./mvnw test
```

If configured:

```bash
./mvnw verify
```

## Deliverable

Create an implementation discovery note containing:

- Existing abstractions that Organization will reuse
- Missing abstractions that must be created
- Confirmed package structure
- Confirmed UUID database type
- Next Flyway migration number
- Confirmed permission naming format
- Confirmed authenticated-principal type
- Confirmed error-response format
- Confirmed event publication approach
- Current build result

## Acceptance Criteria

- No production behavior changes.
- Existing test suite passes.
- All planned reuse points are documented.
- Any conflict between the approved documentation and current code is identified before implementation starts.

## Codex Task Name

```text
ORG-00 Repository Discovery and Baseline
```

---

# Phase 1: Organization Domain Primitives

## Goal

Implement the Organization module's foundational value objects and enums without creating the aggregate yet.

## Scope

Implement or reuse:

```text
OrganizationId
TenantId
UserId
OrganizationName
TaxRegistrationNumber
Address
EmailAddress
PhoneNumber
Website
OrganizationContact
CurrencyCode
OrganizationTimeZone
OrganizationLocale
DateFormat
TimeFormat
WeekStart
OrganizationSettings
OrganizationStatus
AuditMetadata
```

Reuse shared `TenantId`, `UserId`, or audit abstractions where they already exist.

## Required Validation

### OrganizationName

- Legal name required
- Legal name trimmed
- Maximum 200 characters
- Display name required
- Display name trimmed
- Maximum 120 characters

### TaxRegistrationNumber

- Optional
- Trimmed
- Maximum 50 characters

### Address

- Line 1 required
- Line 2 optional
- City required
- State required for the first version
- Postal code required
- Country code normalized to uppercase
- Country code must contain two alphabetic characters

### EmailAddress

- Required
- Trimmed
- Normalized according to existing policy
- Maximum 254 characters
- Practical syntax validation

### PhoneNumber

- Required
- Trimmed
- Maximum 30 characters
- Reject alphabetic input
- Avoid adding a new external library unless approved

### Website

- Optional
- Maximum 500 characters
- Reject unsupported URI schemes
- Reject embedded credentials

### CurrencyCode

- Uppercase normalization
- Validate using `java.util.Currency`

### OrganizationTimeZone

- Validate using `ZoneId.of`

### OrganizationLocale

- Normalize using BCP 47 conventions
- Reject blank or unsupported values according to platform policy

## Tests

Create plain unit tests for every value object.

Minimum test categories:

- Valid input
- Boundary lengths
- Whitespace normalization
- Null input
- Blank input
- Invalid format
- Equality
- Immutability

Do not start Spring.

## Out of Scope

- Organization aggregate
- JPA
- Controllers
- Security
- Flyway
- Events

## Acceptance Criteria

- Domain primitives compile independently of Spring and JPA.
- All validation tests pass.
- Enum values are standardized as:

```text
ACTIVE
SUSPENDED
ARCHIVED
```

```text
TWELVE_HOUR
TWENTY_FOUR_HOUR
```

```text
MONDAY
SUNDAY
SATURDAY
```

- No duplicate shared identifiers are introduced.

## Codex Task Name

```text
ORG-01 Domain Primitives and Value Objects
```

---

# Phase 2: Organization Aggregate and Domain Events

## Goal

Implement the Organization aggregate, aggregate invariants, audit behavior, state transitions, and domain-event recording.

## Scope

Implement:

```text
Organization
OrganizationCreated
OrganizationProfileUpdated
OrganizationSettingsUpdated
OrganizationStatusChanged
```

## Aggregate State

```text
id
tenantId
name
taxRegistrationNumber
address
contact
settings
status
version
auditMetadata
domainEvents
```

## Required Factory Methods

```java
Organization.create(...)
```

```java
Organization.reconstitute(...)
```

Creation must:

- Assign initial state.
- Set status to `ACTIVE`.
- Initialize audit metadata.
- Register `OrganizationCreated`.

Reconstitution must:

- Restore persisted state.
- Preserve version.
- Preserve audit metadata.
- Register no events.

## Required Behavior

```java
updateProfile(...)
updateSettings(...)
activate(...)
suspend(...)
archive(...)
pullDomainEvents()
```

## Business Rules

- Tenant ID is immutable.
- Archived organizations cannot be updated.
- Archived organizations cannot be reactivated.
- No-op profile updates do not alter audit metadata.
- No-op settings updates do not alter audit metadata.
- No-op operations do not publish events.
- Successful changes update `updatedAt` and `updatedBy`.
- Creation metadata remains unchanged after updates.
- Pulled domain events are cleared from the aggregate.

## Tests

Create aggregate tests for:

- Creation
- Reconstitution
- Profile updates
- Settings updates
- Status transitions
- Invalid state transitions
- Audit behavior
- No-op behavior
- Event payloads
- Event clearing

## Out of Scope

- Repository
- Application services
- JPA
- Controllers
- Spring event publication

## Acceptance Criteria

- Aggregate is framework-independent.
- All invariants are tested.
- Reconstitution emits no event.
- Every state-changing operation emits exactly the expected event.
- No-op changes emit no event.

## Codex Task Name

```text
ORG-02 Organization Aggregate and Domain Events
```

---

# Phase 3: Application Ports and Contracts

## Goal

Define the Organization module's application boundaries without implementing HTTP or persistence adapters.

## Scope

Implement application or domain ports for:

```text
OrganizationRepository
CurrentActorProvider
OrganizationAuthorizationService
TimeProvider
DomainEventPublisher
```

Reuse existing equivalents wherever possible.

## Repository Contract

Required methods:

```java
Optional<Organization> findByTenantId(TenantId tenantId);

Optional<Organization> findByIdAndTenantId(
        OrganizationId organizationId,
        TenantId tenantId);

boolean existsByTenantId(TenantId tenantId);

Organization save(Organization organization);
```

## Authenticated Actor Contract

Reuse or define:

```text
userId
tenantId
roles
permissions
```

The Organization module must consume a trusted actor abstraction and must not parse JWT claims.

## Authorization Contract

Define methods such as:

```java
requireProfileRead(AuthenticatedActor actor);

requireProfileUpdate(AuthenticatedActor actor);

requireSettingsRead(AuthenticatedActor actor);

requireSettingsUpdate(AuthenticatedActor actor);
```

## Time Contract

Reuse or define:

```java
Instant now();
```

## Event Contract

Reuse or define:

```java
void publishAll(Collection<? extends DomainEvent> events);
```

## Tests

Only contract-level tests or simple authorization implementation tests are needed in this phase.

## Out of Scope

- Application use cases
- Spring Security adapter
- Spring event publisher
- JPA adapter

## Acceptance Criteria

- Application ports do not depend on infrastructure.
- Existing shared contracts are reused.
- No JWT, HTTP, JPA, or Spring Security types appear in domain code.
- Permission names are confirmed and centralized.

## Codex Task Name

```text
ORG-03 Application Ports and Contracts
```

---

# Phase 4: Profile Application Use Cases

## Goal

Implement profile read and update orchestration.

## Scope

Implement:

```text
GetOrganizationProfileService
UpdateOrganizationProfileService
UpdateOrganizationCommand
OrganizationProfileResponse
Application mapper
```

Names may follow existing project conventions.

## Get Profile Flow

```text
Resolve actor
Authorize organization:read
Load by actor tenant ID
Map to response
Return
```

## Update Profile Flow

```text
Resolve actor
Authorize organization:update
Load by actor tenant ID
Create validated value objects
Invoke aggregate updateProfile
Save aggregate
Publish pulled events
Map to response
Return
```

## Rules

- Tenant ID must come only from the actor.
- Commands must not include tenant ID.
- Repository lookup must be tenant-scoped.
- Events must not be published when save fails.
- Missing Organization must raise `OrganizationNotFoundException`.
- Application service must own the transaction boundary once Spring integration is added.

## Tests

Mock:

- Repository
- Current actor provider
- Authorization service
- Time provider
- Event publisher

Verify:

- Permission check occurs.
- Actor tenant is used.
- Correct repository method is called.
- Aggregate is saved.
- Events are published after save.
- Persistence failure prevents event publication.
- Unauthorized access prevents repository access.
- Missing Organization is handled.

## Out of Scope

- Settings use cases
- Controller
- JPA
- Real Spring transactions

## Acceptance Criteria

- Profile use cases work with mocked ports.
- No tenant identifier is accepted from the caller.
- All application unit tests pass.
- Domain behavior remains inside the aggregate.

## Codex Task Name

```text
ORG-04 Profile Application Use Cases
```

---

# Phase 5: Settings Application Use Cases

## Goal

Implement Organization settings read and update orchestration.

## Scope

Implement:

```text
GetOrganizationSettingsService
UpdateOrganizationSettingsService
UpdateOrganizationSettingsCommand
OrganizationSettingsResponse
```

## Read Settings Flow

```text
Resolve actor
Authorize organization:settings:read
Load by tenant
Map settings
Return
```

## Update Settings Flow

```text
Resolve actor
Authorize organization:settings:update
Load by tenant
Validate settings value objects
Invoke aggregate updateSettings
Save
Publish events
Return
```

## Tests

Verify:

- Correct permission
- Actor-derived tenant
- Currency validation
- Timezone validation
- Locale validation
- Date format mapping
- Time format mapping
- Week-start mapping
- No-op update behavior
- Save and event publication ordering

## Acceptance Criteria

- Settings use cases are independently reviewable.
- Invalid settings fail before persistence.
- No tenant input exists in commands.
- Tests pass without Spring.

## Codex Task Name

```text
ORG-05 Settings Application Use Cases
```

---

# Phase 6: Flyway Schema

## Goal

Introduce the Organization database schema before implementing JPA mappings.

## Scope

Inspect the existing migration history and create the next migration.

Create:

```text
organizations
organization_addresses
organization_contacts
organization_settings
```

## Schema Rules

- Use the existing project UUID representation.
- `organizations.tenant_id` must be unique.
- Child tables must reference `organizations.id`.
- Child tables should use shared primary keys if the approved `@MapsId` design is retained.
- Use `ON DELETE CASCADE` for aggregate-owned child rows.
- Add status constraint where supported.
- Add time-format constraint.
- Add week-start constraint.
- Do not duplicate the tenant unique index.
- Add a status index only if justified by the approved design.

## Validation

Run Flyway against a clean MySQL Testcontainer.

Configure:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

JPA validation may be completed in the next phase once entities exist.

## Tests

Create a migration smoke test that verifies:

- Fresh database migration succeeds.
- Expected tables exist.
- Expected constraints exist.
- Duplicate tenant rows are rejected.
- Invalid foreign keys are rejected where applicable.

## Out of Scope

- JPA entities
- Repository adapter
- Controllers

## Acceptance Criteria

- Migration works from an empty database.
- Existing migrations remain untouched.
- UUID storage matches the existing schema.
- One Organization per tenant is enforced by the database.

## Codex Task Name

```text
ORG-06 Flyway Organization Schema
```

---

# Phase 7: JPA Persistence Entities

## Goal

Implement the persistence entity graph matching the Flyway schema.

## Scope

Implement:

```text
OrganizationJpaEntity
OrganizationAddressJpaEntity
OrganizationContactJpaEntity
OrganizationSettingsJpaEntity
OrganizationStatusJpa
```

## Mapping Rules

Root:

```java
@Entity
@Table(name = "organizations")
```

Required root fields:

```text
id
tenantId
legalName
displayName
taxRegistrationNumber
status
version
createdAt
updatedAt
createdBy
updatedBy
```

Owned relationships:

```java
@OneToOne(
    mappedBy = "organization",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY,
    optional = false
)
```

Child entities should use:

```java
@MapsId
```

when aligned with the migration.

## Design Constraints

- Protected no-argument constructors
- No domain logic
- No public mutable API unless required by the existing mapping convention
- Bidirectional helper methods
- Tenant ID is non-updatable
- Root carries the optimistic-lock version
- Child entities do not need separate versions

## Tests

Create JPA mapping tests verifying:

- Context starts with `ddl-auto=validate`.
- Relationships persist.
- Shared IDs are correct.
- Cascades work.
- Orphan behavior is correct where replacements are supported.

## Out of Scope

- Domain mapper
- Repository adapter
- HTTP

## Acceptance Criteria

- Entity mappings exactly match Flyway.
- Hibernate schema validation passes.
- Full entity graph can be persisted.
- No domain package depends on JPA.

## Codex Task Name

```text
ORG-07 JPA Entity Model
```

---

# Phase 8: Persistence Mapper

## Goal

Implement explicit conversion between the domain aggregate and the JPA entity graph.

## Scope

Implement:

```java
Organization toDomain(OrganizationJpaEntity entity);

OrganizationJpaEntity toNewEntity(Organization organization);

void updateEntity(
        Organization organization,
        OrganizationJpaEntity entity);
```

## Rules

`toDomain` must:

- Use aggregate reconstitution.
- Preserve ID.
- Preserve tenant ID.
- Preserve status.
- Preserve version.
- Preserve audit metadata.
- Reconstruct address, contact, and settings.
- Generate no events.

`toNewEntity` must:

- Create the full entity graph.
- Synchronize both relationship sides.
- Preserve aggregate identifiers.
- Use child shared IDs correctly.

`updateEntity` must:

- Update editable fields.
- Update existing children rather than recreate them unnecessarily.
- Preserve root ID.
- Preserve tenant ID.
- Preserve created metadata.
- Leave JPA version management to Hibernate.
- Maintain bidirectional relationships.

## Tests

Create mapper tests for:

- Domain to new JPA entity
- JPA to domain
- Round-trip equality
- Update mapping
- Immutable field preservation
- Child entity preservation
- No domain events after mapping

## Acceptance Criteria

- Mapper contains no business validation beyond safe null handling.
- Round-trip mapping preserves all aggregate state.
- Reconstitution publishes no events.
- Managed update semantics are tested.

## Codex Task Name

```text
ORG-08 Persistence Mapper
```

---

# Phase 9: Spring Data Repository and Adapter

## Goal

Connect the domain repository port to Spring Data JPA.

## Scope

Implement:

```text
SpringDataOrganizationRepository
OrganizationRepositoryAdapter
```

## Required Queries

```java
@EntityGraph(attributePaths = {
        "address",
        "contact",
        "settings"
})
Optional<OrganizationJpaEntity> findByTenantId(UUID tenantId);
```

```java
@EntityGraph(attributePaths = {
        "address",
        "contact",
        "settings"
})
Optional<OrganizationJpaEntity> findByIdAndTenantId(
        UUID id,
        UUID tenantId);
```

```java
boolean existsByTenantId(UUID tenantId);
```

## Save Strategy

For an existing aggregate:

1. Load the managed entity by organization ID and tenant ID.
2. Apply `updateEntity`.
3. Allow dirty checking.
4. Return reconstructed domain state.

For a new aggregate:

1. Use `toNewEntity`.
2. Persist full graph.
3. Return reconstructed domain state.

Do not determine newness only from version `0` unless existing project behavior guarantees that approach.

## Exception Translation

Translate:

- Duplicate tenant constraint
- Optimistic locking
- Data integrity violations
- Generic persistence failures

Do not expose constraint names or SQL messages.

## Integration Tests

Verify:

- Save new aggregate
- Load by tenant
- Load complete graph
- Update existing aggregate
- Child rows are not duplicated
- Unique tenant constraint
- Tenant-scoped lookup
- Version persistence
- Audit persistence
- No lazy-loading failures after repository return

## Acceptance Criteria

- Domain repository port is fully implemented.
- Tenant-facing queries are tenant-scoped.
- Repository integration tests use MySQL Testcontainers.
- No JPA types leak into application code.

## Codex Task Name

```text
ORG-09 Repository Adapter and Persistence Integration
```

---

# Phase 10: Profile REST API

## Goal

Expose profile read and update APIs without implementing settings endpoints yet.

## Scope

Implement:

```http
GET /api/v1/organizations/me
PUT /api/v1/organizations/me
```

Implement:

```text
OrganizationController
UpdateOrganizationRequest
AddressRequest
ContactRequest
OrganizationProfileResponse
Presentation mapper
```

## Request Rules

The request must not accept:

```text
tenantId
organizationId
createdAt
createdBy
updatedAt
updatedBy
version
status
```

unless a version field is deliberately introduced later for API-level concurrency.

## Validation

Use Jakarta Bean Validation for structural rules:

- Required fields
- Maximum lengths
- Nested validation
- Basic email formatting

Domain value objects remain the final source of business validation.

## Controller Tests

Verify:

- 200 for successful GET
- 200 for successful PUT
- 400 for invalid input
- Response field names
- Enum serialization where applicable
- No persistence entity exposure
- No tenant field accepted from client
- Service interaction

Security may be mocked or temporarily configured according to project test conventions; complete security verification occurs in Phase 12.

## Acceptance Criteria

- Profile endpoints work through application services.
- Controller contains no business logic.
- Request and application command types remain separated where project conventions require it.
- API tests pass.

## Codex Task Name

```text
ORG-10 Profile REST API
```

---

# Phase 11: Settings REST API

## Goal

Expose Organization settings read and update APIs.

## Scope

Implement:

```http
GET /api/v1/organizations/me/settings
PUT /api/v1/organizations/me/settings
```

Implement:

```text
UpdateOrganizationSettingsRequest
OrganizationSettingsResponse
Controller methods
Presentation mapping
```

## Validation

Verify:

- Currency code shape
- Timezone required
- Locale required
- Date format required
- Time format required
- Week start required

Domain factories must perform semantic validation.

## Controller Tests

Test:

- Valid settings read
- Valid settings update
- Invalid currency
- Invalid timezone
- Invalid locale
- Invalid date format
- Invalid time format
- Invalid week start
- Missing required field
- Stable JSON serialization

## Acceptance Criteria

- Settings endpoints are complete.
- Validation failures return the current platform validation contract.
- No tenant scope is accepted from the client.
- All controller tests pass.

## Codex Task Name

```text
ORG-11 Settings REST API
```

---

# Phase 12: Security Integration

## Goal

Integrate Organization endpoints with OdinSync's existing `oauth2ResourceServer` authentication flow and permission model.

## Scope

Implement or connect:

```text
SpringSecurityCurrentActorAdapter
Organization authorization implementation
Controller method security
```

Reuse the existing authenticated principal.

Do not parse raw JWT claims inside Organization services or controllers.

## Permissions

```text
organization:read
organization:update
organization:settings:read
organization:settings:update
```

Confirm exact delimiter and naming convention used by the existing Identity module before implementation.

## Defense in Depth

Enforce permissions at:

- Controller or method-security boundary
- Application authorization service

## Security Tests

For every endpoint, verify:

- Missing token returns 401.
- Invalid token returns 401 where covered by integration configuration.
- Valid authentication without permission returns 403.
- Unrelated permission returns 403.
- Required permission succeeds.
- Application service is not invoked after authentication or authorization rejection.
- Actor tenant and user IDs are derived from the trusted principal.

## Acceptance Criteria

- Organization code does not parse JWTs.
- Current actor is resolved through a stable application port.
- Permission matrix is fully tested.
- All four endpoints are protected.

## Codex Task Name

```text
ORG-12 Security and Permission Integration
```

---

# Phase 13: Error Handling and Problem Details

## Goal

Complete stable Organization error mapping using the platform-wide error model.

## Scope

Implement or register:

```text
OrganizationNotFoundException
OrganizationAlreadyExistsException
InvalidOrganizationProfileException
UnsupportedCurrencyException
InvalidOrganizationTimeZoneException
UnsupportedOrganizationLocaleException
OrganizationAccessDeniedException
OrganizationConcurrentUpdateException
OrganizationStateConflictException
OrganizationPersistenceException
```

## Error Catalogue

| Code | HTTP Status |
|---|---:|
| `ORG_001` | 404 |
| `ORG_002` | 400 |
| `ORG_003` | 400 |
| `ORG_004` | 400 |
| `ORG_005` | 400 |
| `ORG_006` | 403 |
| `ORG_007` | 409 |
| `ORG_008` | 409 |
| `ORG_009` | 409 |
| `ORG_010` | 500 |

## Tests

Verify:

- Correct status
- Correct error code
- Correct content type
- Request path
- Trace or correlation ID where supported
- Validation field errors
- No stack trace
- No SQL
- No constraint name
- No Java implementation details

## Acceptance Criteria

- All documented Organization failures map predictably.
- Platform-wide handling is reused.
- Security errors remain owned by Identity or shared security infrastructure.
- Error-contract tests pass.

## Codex Task Name

```text
ORG-13 Error Contract and Exception Mapping
```

---

# Phase 14: Tenant-Isolation Hardening

## Goal

Prove and harden tenant isolation across the application, persistence, and HTTP layers.

## Scope

Add dedicated security tests and correct any unsafe access paths.

## Required Tests

Create Tenant A and Tenant B.

Verify:

- Tenant A reads Organization A.
- Tenant B reads Organization B.
- Tenant A cannot retrieve Organization B by ID.
- Tenant A cannot update Organization B.
- Actor tenant is always passed to the repository.
- Public commands and requests contain no tenant ID.
- Malicious unknown tenant fields are rejected or ignored according to Jackson policy.
- A second Organization for the same tenant is rejected.
- Cross-tenant IDs do not reveal resource existence.
- No tenant-facing service calls unscoped `findById`.

## Static Review

Search the module for:

```text
findById(
tenantId request fields
X-Tenant-ID
raw tenant headers
```

Document every legitimate exception.

## Acceptance Criteria

- Tenant-isolation tests exist at multiple layers.
- No cross-tenant read or write succeeds.
- Tenant scope is derived exclusively from the authenticated actor for self-service endpoints.
- Database uniqueness remains the final one-Organization-per-tenant safeguard.

## Codex Task Name

```text
ORG-14 Tenant-Isolation Hardening
```

---

# Phase 15: Optimistic Locking and Concurrency

## Goal

Verify lost-update protection and stable conflict handling.

## Scope

Complete:

```java
@Version
```

behavior and exception translation.

## Required Tests

Using two independent persistence contexts:

1. Load the same Organization twice.
2. Update the first instance.
3. Commit or flush.
4. Update the second instance.
5. Confirm stale update rejection.

Verify:

- Root version increments.
- No data is silently overwritten.
- Adapter translates the persistence exception.
- API returns `409 Conflict`.
- Error code is `ORG_007`.
- Conflict does not trigger automatic unbounded retries.

## Additional Scenario

Verify that simultaneous profile and settings updates conflict when using one aggregate root version.

Document that this is intentional.

## Acceptance Criteria

- Optimistic locking works against MySQL.
- Conflict translation is stable.
- No server-side blind retry is added.
- Concurrency tests are deterministic.

## Codex Task Name

```text
ORG-15 Optimistic Locking and Conflict Handling
```

---

# Phase 16: Domain Event Publication

## Goal

Publish aggregate events only after successful persistence using the existing OdinSync event mechanism.

## Scope

Implement or connect:

```text
DomainEventPublisher
SpringDomainEventPublisher
After-commit listeners where required
```

## Required Behavior

- Save aggregate before publishing.
- Publish all pulled events exactly once per successful use-case invocation.
- Do not publish after persistence failure.
- Do not publish after transaction rollback.
- Clear pulled events.
- Do not add Kafka.
- Do not implement an outbox in this phase.

## Tests

Verify:

- OrganizationCreated publication
- ProfileUpdated publication
- SettingsUpdated publication
- StatusChanged publication
- No event on no-op updates
- No publication when save fails
- After-commit listener does not run on rollback
- Event payload contains event ID, tenant ID, organization ID, actor ID, and timestamp

## Documentation

Add an explicit follow-up note for transactional outbox implementation when durable cross-service event delivery is introduced.

## Acceptance Criteria

- Publication semantics are deterministic.
- No external broker call occurs inside the Organization database transaction.
- Event tests pass.
- Outbox remains deferred.

## Codex Task Name

```text
ORG-16 Domain Event Publication
```

---

# Phase 17: Observability

## Goal

Add production diagnostics without introducing high-cardinality or sensitive telemetry.

## Scope

Integrate with existing:

- Logging
- Micrometer
- OpenTelemetry
- Actuator

Only add custom instrumentation where platform conventions already support it.

## Logging

Add structured logs for:

```text
organization.profile.read
organization.profile.updated
organization.settings.read
organization.settings.updated
organization.update.conflict
organization.persistence.failure
organization.access.denied
```

Include where supported:

```text
traceId
spanId
tenantId
organizationId
actorId
operation
result
errorCode
```

Do not log:

- Tokens
- Authorization headers
- Full tax identifiers
- Full addresses
- Full phone numbers
- Credentials

## Metrics

Candidate metrics:

```text
odinsync.organization.profile.read
odinsync.organization.profile.update
odinsync.organization.settings.read
odinsync.organization.settings.update
odinsync.organization.update.conflict
odinsync.organization.persistence.failure
```

Allowed tags:

```text
operation
result
error_code
```

Forbidden high-cardinality tags:

```text
tenant_id
organization_id
user_id
email
```

## Tracing

Ensure Organization operations participate in existing HTTP and database traces.

Do not place tracing APIs in the domain layer.

## Tests

Where feasible, verify:

- Counter increment
- Failure metric
- Conflict metric
- Sensitive fields absent from log output
- Trace context retained
- Health endpoint remains functional

## Acceptance Criteria

- Telemetry follows existing platform conventions.
- No sensitive information is logged.
- No high-cardinality metric dimensions are added.
- Observability tests pass where practical.

## Codex Task Name

```text
ORG-17 Organization Observability
```

---

# Phase 18: Architecture and End-to-End Verification

## Goal

Verify complete workflows and enforce module boundaries.

## Scope

Add ArchUnit rules where supported.

## Architecture Rules

- Domain does not depend on Spring.
- Domain does not depend on JPA.
- Application does not depend on presentation.
- Application does not depend directly on infrastructure implementations.
- Controllers do not access Spring Data repositories.
- JPA entities remain in infrastructure.
- Other bounded contexts do not access Organization persistence classes.
- Business rules do not exist in controllers or JPA entities.

## End-to-End Scenarios

### Read Profile

```text
Valid token
Required permission
Organization exists
GET profile
200 response
```

### Update Profile

```text
Valid token
Update permission
Valid request
Aggregate updated
Database updated
Audit updated
Event published
200 response
```

### Read and Update Settings

Verify complete protected workflows.

### Unauthorized

Verify 401 and 403 behavior.

### Cross-Tenant

Verify Tenant A cannot read or update Tenant B.

### Concurrent Update

Verify one update succeeds and the stale update returns 409.

### Validation

Verify invalid inputs produce the platform Problem Details response.

## Acceptance Criteria

- Complete workflows pass through actual application wiring.
- Architecture rules pass.
- No module-boundary violations remain.
- MySQL Testcontainers are used for database-dependent workflows.

## Codex Task Name

```text
ORG-18 Architecture and End-to-End Verification
```

---

# Phase 19: Documentation Reconciliation and Release Readiness

## Goal

Align completed implementation with the approved documentation and prepare the module for merge.

## Scope

Review:

- Domain documentation
- API documentation
- Permission catalogue
- Error-code catalogue
- Event catalogue
- Flyway schema documentation
- Package structure
- Test strategy
- Operational notes
- OpenAPI specification

## Required Final Validation

Run:

```bash
./mvnw clean verify
```

Also run configured quality checks:

```bash
./mvnw checkstyle:check
./mvnw spotless:check
```

Use only commands actually configured in the repository.

## Final Report

Produce:

- Files created
- Files modified
- Tests added
- Build results
- Architecture decisions
- Deviations from documentation
- Deferred capabilities
- Known limitations
- Follow-up tasks

## Deferred Follow-Up Items

Do not implement these merely to close the module:

- Transactional outbox
- Kafka integration
- Redis caching
- Authorization-version validation
- Organization branding
- Branches
- Multi-office support
- Hard deletion
- Audit-history table
- Platform administrator APIs
- Dedicated tenant databases

## Acceptance Criteria

- Full build passes.
- Documentation matches code.
- OpenAPI reflects actual endpoints.
- No unfinished placeholder code remains.
- Deferred items are explicitly tracked.
- Module is ready for code review and merge.

## Codex Task Name

```text
ORG-19 Documentation Reconciliation and Release Readiness
```

---

# Recommended Execution Groups

The phases should normally be executed one Codex task at a time.

For practical implementation, the following review checkpoints are recommended.

## Checkpoint 1: Domain Foundation

Complete:

```text
ORG-00
ORG-01
ORG-02
```

Review:

- Domain terminology
- Value-object validation
- Aggregate invariants
- Event semantics

Do not proceed until the aggregate design is accepted.

---

## Checkpoint 2: Application Layer

Complete:

```text
ORG-03
ORG-04
ORG-05
```

Review:

- Port boundaries
- Tenant propagation
- Permission design
- Use-case orchestration
- Transaction ownership

---

## Checkpoint 3: Persistence

Complete:

```text
ORG-06
ORG-07
ORG-08
ORG-09
```

Review:

- Schema
- UUID representation
- JPA mapping
- Mapper correctness
- Tenant-scoped queries
- Integration-test reliability

---

## Checkpoint 4: API and Security

Complete:

```text
ORG-10
ORG-11
ORG-12
ORG-13
ORG-14
```

Review:

- HTTP contracts
- Authentication
- Permissions
- Problem Details
- Tenant-isolation guarantees

---

## Checkpoint 5: Production Readiness

Complete:

```text
ORG-15
ORG-16
ORG-17
ORG-18
ORG-19
```

Review:

- Concurrency
- Events
- Observability
- Architecture rules
- End-to-end workflows
- Documentation accuracy

---

# Phase Dependency Map

```text
ORG-00
   │
   ▼
ORG-01
   │
   ▼
ORG-02
   │
   ▼
ORG-03
   ├──────────────┐
   ▼              ▼
ORG-04          ORG-05
   └──────┬───────┘
          ▼
        ORG-06
          │
          ▼
        ORG-07
          │
          ▼
        ORG-08
          │
          ▼
        ORG-09
          ├──────────────┐
          ▼              ▼
        ORG-10          ORG-11
          └──────┬───────┘
                 ▼
               ORG-12
                 │
                 ▼
               ORG-13
                 │
                 ▼
               ORG-14
                 │
                 ▼
               ORG-15
                 │
                 ▼
               ORG-16
                 │
                 ▼
               ORG-17
                 │
                 ▼
               ORG-18
                 │
                 ▼
               ORG-19
```

---

# Rules for Every Codex Task

Each task prompt should instruct Codex to:

1. Read the current repository before changing code.
2. Review the prior phase implementation.
3. Implement only the requested phase.
4. Avoid future-phase work.
5. Reuse existing abstractions.
6. Add tests for every new behavior.
7. Run the relevant build commands.
8. Report all changed files.
9. Report test results honestly.
10. Document blockers without bypassing architecture rules.

Codex must not:

- Rewrite unrelated modules.
- Rename existing shared contracts without need.
- Modify applied Flyway migrations.
- Introduce Kafka, Redis, or an outbox early.
- Add tenant identifiers to public self-service requests.
- Parse JWTs inside Organization code.
- Expose JPA entities outside infrastructure.
- Disable failing tests.
- weaken validation merely to make tests pass.
- Use `ddl-auto=update`.
- Claim success without executing tests.

---

# Initial Implementation Recommendation

Start with:

```text
ORG-00 Repository Discovery and Baseline
```

Do not begin domain implementation until ORG-00 confirms:

- Existing shared ID types
- Current authentication principal
- Permission naming
- Error handling
- UUID storage
- Flyway sequence
- Testing conventions

After ORG-00 is reviewed, proceed to:

```text
ORG-01 Domain Primitives and Value Objects
```

This sequence minimizes rework and prevents the Organization module from duplicating infrastructure already present in OdinSync.