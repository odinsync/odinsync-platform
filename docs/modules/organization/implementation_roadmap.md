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
- Auditing
- Security permissions

Each phase should end with:

```bash
./mvnw test
```

Where integration tests are included:

```bash
./mvnw verify
```

---

# Roadmap Overview

| Phase | Scope | Primary Output |
|---|---|---|
| Phase 0 | Repository analysis | Implementation decision record |
| Phase 1 | Domain primitives | Value objects and domain exceptions |
| Phase 2 | Aggregate design | Organization aggregate and domain events |
| Phase 3 | Application contracts | Ports, commands, queries, and DTOs |
| Phase 4 | Application use cases | Profile and settings services |
| Phase 5 | Persistence model | JPA entities and mappings |
| Phase 6 | Database schema | Flyway migration |
| Phase 7 | Repository implementation | Spring Data repository and adapter |
| Phase 8 | REST API | Requests, responses, controllers |
| Phase 9 | Security integration | Actor resolution and permissions |
| Phase 10 | Error handling | Problem Details and exception mapping |
| Phase 11 | Integration verification | Repository and migration tests |
| Phase 12 | Security verification | Authentication and authorization tests |
| Phase 13 | Tenant isolation | Cross-tenant protection tests |
| Phase 14 | Concurrency | Optimistic-locking behavior |
| Phase 15 | Event publication | Transactional domain event delivery |
| Phase 16 | Observability | Logs, metrics, and tracing |
| Phase 17 | Architecture and E2E | ArchUnit and complete workflows |
| Phase 18 | Final hardening | Review, cleanup, and documentation sync |

---

# Phase 0: Repository Analysis and Implementation Baseline

## Goal

Understand the existing OdinSync architecture before adding Organization code.

## Tasks

Inspect:

- Existing module package structures
- Identity domain model
- Shared identifier types
- JWT principal type
- `CurrentActorProvider`
- Permission format
- Global exception handler
- Problem Details implementation
- Audit metadata conventions
- Existing domain-event abstraction
- Existing time-provider abstraction
- Spring Data repository patterns
- UUID persistence format
- Flyway migration history
- Testcontainers setup
- Maven test profiles
- Formatting and static-analysis configuration

Run:

```bash
./mvnw test
```

## Deliverable

Create a short implementation note containing:

- Existing abstractions that will be reused
- Missing abstractions that need to be introduced
- Final package structure
- UUID storage decision
- Security principal integration decision
- Flyway migration number
- Test execution strategy
- Any differences between documentation and current codebase

## Acceptance Criteria

- No Organization production code is added yet.
- Existing tests pass.
- No shared abstraction is duplicated.
- Implementation decisions are documented.

---

# Phase 1: Domain Value Objects and Exceptions

## Goal

Implement the immutable domain primitives required by the Organization aggregate.

## Scope

Implement or reuse:

```text
OrganizationId
OrganizationName
TaxRegistrationNumber
Address
EmailAddress
PhoneNumber
Website
CurrencyCode
OrganizationTimeZone
OrganizationLocale
DateFormat
TimeFormat
WeekStart
OrganizationSettings
OrganizationContact
OrganizationStatus
AuditMetadata
```

Reuse existing:

```text
TenantId
UserId
```

when already available.

## Validation Rules

Implement:

- Required and maximum-length rules
- Input trimming
- Email normalization
- Currency validation using `Currency`
- Timezone validation using `ZoneId`
- Locale normalization using BCP 47
- Website scheme validation
- Country code normalization
- Enum validation

## Tests

Create focused tests for each value object.

Examples:

```text
OrganizationNameTest
AddressTest
EmailAddressTest
CurrencyCodeTest
OrganizationTimeZoneTest
OrganizationLocaleTest
OrganizationSettingsTest
```

## Out of Scope

Do not implement:

- Organization aggregate
- JPA entities
- Controllers
- Security
- Repositories

## Acceptance Criteria

- Domain primitives are immutable.
- Invalid values fail with domain-specific exceptions.
- Domain code has no Spring or JPA dependencies.
- All value-object tests pass.
- Full Maven unit test suite passes.

---

# Phase 2: Organization Aggregate and Domain Events

## Goal

Implement the Organization aggregate root and its business behavior.

## Scope

Implement:

```text
Organization
OrganizationCreated
OrganizationProfileUpdated
OrganizationSettingsUpdated
OrganizationStatusChanged
```

Required aggregate operations:

```text
create
reconstitute
updateProfile
updateSettings
activate
suspend
archive
pullDomainEvents
```

## Business Rules

Enforce:

- Tenant ownership is immutable.
- Archived organizations cannot be modified.
- No-op updates do not create events.
- Profile changes update audit metadata.
- Settings changes update audit metadata.
- Status transitions are explicit.
- Reconstitution does not publish creation events.
- Creation initializes audit metadata and status.

## Tests

Cover:

- Successful creation
- Initial active status
- Audit initialization
- Creation event
- Profile update
- Settings update
- Status transitions
- Archived-state restrictions
- No-op update
- Event clearing
- Reconstitution behavior

## Out of Scope

Do not implement:

- Application services
- Persistence
- HTTP
- Security integration

## Acceptance Criteria

- Aggregate invariants are enforced.
- Creation and reconstitution are separate.
- Domain events contain required metadata.
- Domain tests pass without Spring.
- No infrastructure imports exist in the domain package.

---

# Phase 3: Application Contracts and Ports

## Goal

Define the Organization module’s application boundaries without implementing orchestration yet.

## Scope

Implement input models:

```text
UpdateOrganizationCommand
UpdateOrganizationSettingsCommand
AddressCommand
ContactCommand
ProvisionOrganizationCommand
```

Implement output models:

```text
OrganizationResponse
OrganizationSettingsResponse
OrganizationSummary
AddressResponse
ContactResponse
```

Implement ports:

```text
OrganizationRepository
CurrentActorProvider
OrganizationAuthorizationService
DomainEventPublisher
TimeProvider
```

Reuse existing platform ports wherever possible.

Implement query or use-case interfaces:

```text
GetOrganizationProfileUseCase
UpdateOrganizationProfileUseCase
GetOrganizationSettingsUseCase
UpdateOrganizationSettingsUseCase
ProvisionOrganizationUseCase
```

## Public Contract

Add a narrow cross-module query contract if required:

```text
OrganizationQuery
OrganizationSummary
```

Do not expose the aggregate directly to other modules.

## Tests

Add contract-level tests only where meaningful, such as:

- DTO immutability
- Mapper conversion
- Command normalization policy

## Out of Scope

Do not implement:

- Application service logic
- JPA
- Controllers
- Security adapters

## Acceptance Criteria

- Application contracts compile.
- Ports contain no Spring Data or HTTP types.
- Tenant ID is absent from public self-service commands.
- Cross-module contracts expose only required fields.
- Existing tests continue to pass.

---

# Phase 4: Application Use Cases

## Goal

Implement Organization profile and settings orchestration.

## Scope

Implement:

```text
GetOrganizationProfileService
UpdateOrganizationProfileService
GetOrganizationSettingsService
UpdateOrganizationSettingsService
```

Optionally implement provisioning only if the integration point is already clear:

```text
ProvisionOrganizationService
```

## Required Flow

For reads:

```text
Resolve actor
Authorize
Load by actor tenant
Map response
```

For updates:

```text
Resolve actor
Authorize
Load by actor tenant
Create value objects
Invoke aggregate behavior
Save aggregate
Publish events
Map response
```

## Permissions

Use:

```text
organization:read
organization:update
organization:settings:read
organization:settings:update
```

## Tests

Mock:

- Repository
- Current actor provider
- Authorization service
- Time provider
- Domain event publisher

Verify:

- Correct permission checked
- Tenant comes from actor
- Repository uses tenant-scoped lookup
- Aggregate is saved
- Events publish only after successful save
- Missing organization is handled
- Validation failure prevents persistence
- Unauthorized operation does not persist

## Out of Scope

Do not implement:

- Controllers
- JPA
- Flyway
- SecurityContext access

## Acceptance Criteria

- All four main use cases work through mocked ports.
- Tenant ID never comes from a command.
- Transaction annotations, if used, remain in application services.
- Application tests pass.

---

# Phase 5: JPA Persistence Entities

## Goal

Implement the persistence representation of the aggregate.

## Scope

Implement:

```text
OrganizationJpaEntity
OrganizationAddressJpaEntity
OrganizationContactJpaEntity
OrganizationSettingsJpaEntity
OrganizationStatusJpa
```

Use:

- Root `@Version`
- One-to-one ownership
- Lazy loading
- Cascade persistence
- Orphan removal
- Shared child primary keys with `@MapsId` where approved
- Relationship helper methods

## Design Rules

JPA entities must:

- Remain under infrastructure
- Contain no domain behavior
- Contain no authorization logic
- Never be returned from controllers
- Never be imported by application services

## Tests

Add lightweight entity relationship tests where useful.

Verify:

- Assigning child synchronizes both relationship sides.
- Tenant ID cannot be updated.
- Shared child identity is maintained.

## Out of Scope

Do not implement:

- Flyway
- Spring Data repository
- Repository adapter
- REST API

## Acceptance Criteria

- Entity graph compiles.
- Relationship ownership is correct.
- Root version field is configured.
- Domain remains independent of JPA.

---

# Phase 6: Flyway Database Migration

## Goal

Create the Organization schema.

## Scope

Create the next Flyway migration for:

```text
organizations
organization_addresses
organization_contacts
organization_settings
```

Use the UUID format already established in OdinSync.

Add:

- Primary keys
- Unique tenant constraint
- Child foreign keys
- `ON DELETE CASCADE`
- Status constraint
- Time-format constraint
- Week-start constraint
- Required indexes

## Required Review

Verify:

- No duplicate tenant index
- Column names match JPA exactly
- Enum values match domain and API exactly
- Timestamp precision matches existing schema
- Foreign key to tenant is added only if consistent with current architecture

## Tests

Start a clean MySQL Testcontainer and run Flyway.

Verify:

- Migration applies successfully
- Tables exist
- Constraints exist
- Migration checksum validation succeeds

## Acceptance Criteria

- Migration works from an empty database.
- Migration follows the current sequence.
- Existing migrations are not modified.
- Hibernate validation can match the new schema once repositories are wired.

---

# Phase 7: Persistence Mapper and Repository Adapter

## Goal

Connect the domain aggregate to JPA persistence.

## Scope

Implement:

```text
OrganizationPersistenceMapper
SpringDataOrganizationRepository
OrganizationRepositoryAdapter
```

Required mapper operations:

```text
toDomain
toNewEntity
updateEntity
```

Required repository methods:

```text
findByTenantId
findByIdAndTenantId
existsByTenantId
save
```

Use `@EntityGraph` or an explicit fetch-join query.

## Mapping Rules

Preserve:

- Organization ID
- Tenant ID
- Child IDs
- Version
- Audit metadata
- Status
- Address
- Contact
- Settings

Do not:

- Publish events
- Revalidate authorization
- Call aggregate creation during reconstitution
- Replace managed children unnecessarily
- Update immutable tenant ownership

## Tests

Add:

- Domain-to-JPA mapping test
- JPA-to-domain mapping test
- Round-trip test
- Managed-entity update test
- Reconstitution event test

## Acceptance Criteria

- Complete aggregate can be mapped both directions.
- Reconstitution generates no domain event.
- Repository adapter exposes only domain objects.
- Tenant-facing lookups are tenant-scoped.
- Unit tests pass.

---

# Phase 8: REST Request, Response, and Controllers

## Goal

Expose the documented Organization APIs.

## Scope

Implement:

```http
GET /api/v1/organizations/me
PUT /api/v1/organizations/me
GET /api/v1/organizations/me/settings
PUT /api/v1/organizations/me/settings
```

Implement:

- Request records
- Response records
- Presentation mapper
- Controller
- Jakarta Bean Validation

## Rules

- No tenant ID in self-service request models.
- No JPA entity exposure.
- No domain behavior inside controllers.
- Controller delegates directly to use cases.
- Enum serialization remains consistent.

## Tests

Use `@WebMvcTest` and MockMvc.

Verify:

- Successful responses
- Request-body mapping
- Required fields
- Maximum lengths
- Invalid enum values
- JSON field names
- Content type

Security behavior may be completed in Phase 9.

## Acceptance Criteria

- All routes exist.
- Request and response contracts match documentation.
- Controllers contain no persistence logic.
- Controller contract tests pass.

---

# Phase 9: Security Integration and Permission Enforcement

## Goal

Connect Organization use cases to OdinSync authentication and authorization.

## Scope

Integrate the existing:

- OAuth2 resource server
- Authenticated principal
- SecurityContext
- Current actor abstraction
- Permission convention

Implement or adapt:

```text
SpringSecurityCurrentActorProvider
DefaultOrganizationAuthorizationService
```

Add method-level security where consistent with project conventions.

## Permission Matrix

| Endpoint | Permission |
|---|---|
| GET profile | `organization:read` |
| PUT profile | `organization:update` |
| GET settings | `organization:settings:read` |
| PUT settings | `organization:settings:update` |

## Rules

- Organization code does not parse JWT claims directly.
- Application authorization uses permissions, not hard-coded roles.
- Actor tenant ID is trusted only after security validation.
- Future `authorization_version` enforcement remains in Identity/security infrastructure.

## Tests

Verify:

- No token returns 401.
- Wrong permission returns 403.
- Correct permission succeeds.
- Similar but incorrect permission fails.
- Actor tenant comes from the authenticated principal.

## Acceptance Criteria

- All endpoints are protected.
- Permission checks are explicit.
- Authentication failures are handled by platform security.
- Security tests pass.

---

# Phase 10: Error Handling and Problem Details

## Goal

Expose stable, safe Organization error responses.

## Scope

Implement or register mappings for:

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

Use the existing platform-wide Problem Details implementation.

## Error Codes

Use:

```text
ORG_001 through ORG_010
```

as defined in the documentation.

## Tests

Verify:

- Correct HTTP status
- Correct error code
- Correct title
- Request path
- Trace ID
- Validation error list
- No SQL details
- No stack traces
- No internal class names

## Acceptance Criteria

- Error responses are stable and safe.
- Authentication errors remain platform-owned.
- Infrastructure exceptions are translated.
- Problem Details tests pass.

---

# Phase 11: Repository and Migration Integration Tests

## Goal

Verify JPA, Flyway, and MySQL behavior together.

## Scope

Use:

- Testcontainers
- Production-compatible MySQL version
- Flyway
- Real JPA mappings
- Real repository adapter

## Required Tests

- Save full aggregate
- Load by tenant
- Load complete entity graph
- Update root fields
- Update child fields
- Preserve child identities
- Preserve tenant ID
- Persist audit metadata
- Unique organization per tenant
- Foreign-key behavior
- Cascade behavior
- Hibernate schema validation
- No domain event on reconstitution
- No N+1 loading behavior

## Acceptance Criteria

- All persistence integration tests pass.
- H2 is not used as the primary compatibility database.
- The schema and JPA mappings agree.
- No duplicate child rows are created.

---

# Phase 12: Complete Controller and Security Tests

## Goal

Verify the protected HTTP contract end to end through the MVC layer.

## Scope

Build a complete matrix for all four endpoints.

For each endpoint test:

- Unauthenticated request
- Missing permission
- Wrong permission
- Correct permission
- Invalid request
- Organization missing
- Successful request
- Safe error response

Use the actual OdinSync principal structure where practical.

## Acceptance Criteria

- All permission combinations behave correctly.
- Application services are not invoked for rejected requests.
- Security and controller contracts are fully covered.
- MockMvc tests pass.

---

# Phase 13: Tenant-Isolation Hardening

## Goal

Prove that Organization data cannot cross tenant boundaries.

## Required Scenarios

Create:

```text
Tenant A → Organization A
Tenant B → Organization B
```

Verify:

- Tenant A reads only Organization A.
- Tenant B reads only Organization B.
- Tenant A cannot update Organization B.
- ID-plus-tenant lookups return empty across tenants.
- Tenant ID supplied through malicious JSON is rejected or ignored.
- Unsupported tenant headers do not change scope.
- A second organization for the same tenant is rejected.
- Cross-tenant resource existence is not disclosed.

## Layers

Test at:

- Application service level
- Repository level
- HTTP level
- Full integration level

## Acceptance Criteria

- Mandatory cross-tenant tests pass.
- No tenant-facing workflow uses unrestricted `findById`.
- Tenant scope always originates from the authenticated actor.

---

# Phase 14: Optimistic Locking and Concurrency

## Goal

Prevent silent lost updates.

## Scope

Verify root-level optimistic locking with:

```java
@Version
```

## Required Scenario

1. Load the same aggregate in two independent transactions.
2. Update transaction A.
3. Commit transaction A.
4. Update transaction B.
5. Commit transaction B.
6. Verify conflict.
7. Translate conflict to Organization exception.
8. Return HTTP 409.

## Additional Scenario

Verify that profile and settings updates conflict when both use the same stale root version.

Document that this is expected because the aggregate uses one consistency boundary.

## Acceptance Criteria

- No committed state is silently overwritten.
- Persistence exception is translated.
- API returns 409.
- Concurrency tests pass reliably.

---

# Phase 15: Domain Event Publication

## Goal

Publish Organization domain events safely after persistence.

## Scope

Wire the existing domain-event publisher.

Support:

```text
OrganizationCreated
OrganizationProfileUpdated
OrganizationSettingsUpdated
OrganizationStatusChanged
```

Use after-commit listeners where appropriate.

## Required Tests

- Event generated for real change
- No event for no-op update
- Events published after successful persistence
- Events not published if persistence fails
- Events cleared after publication
- Listener does not run after rollback
- Event metadata is correct

## Deferred

Do not implement:

- Kafka
- Transactional outbox
- Dead-letter queues
- External event schemas

Record these as future enhancement tasks.

## Acceptance Criteria

- Event behavior is deterministic.
- Failed writes do not publish events.
- Successful updates publish expected events.
- Event tests pass.

---

# Phase 16: Observability and Operational Hooks

## Goal

Make Organization operations observable in production.

## Scope

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

Add Micrometer metrics only according to existing project conventions.

Recommended metrics:

```text
odinsync.organization.profile.read
odinsync.organization.profile.update
odinsync.organization.settings.read
odinsync.organization.settings.update
odinsync.organization.update.conflict
odinsync.organization.persistence.failure
```

## Rules

Do not log:

- JWTs
- Authorization headers
- Full addresses
- Full phone numbers
- Full tax identifiers

Do not use as metric tags:

- Tenant ID
- Organization ID
- User ID

## Tests

Verify where practical:

- Success counter increments
- Failure counter increments
- Conflict counter increments
- Logs include operation context
- Sensitive values are absent
- Trace context is preserved

## Acceptance Criteria

- Key operations are observable.
- Metrics avoid high-cardinality tags.
- Sensitive data is not exposed.
- Existing health endpoints remain functional.

---

# Phase 17: Architecture Tests and End-to-End Workflows

## Goal

Verify architectural boundaries and complete workflows.

## ArchUnit Rules

Enforce:

- Domain does not depend on Spring.
- Domain does not depend on JPA.
- Application does not depend on presentation.
- Application does not depend directly on infrastructure implementations.
- Controllers do not access Spring Data repositories.
- Other modules do not access Organization JPA entities.
- Business logic does not reside in controllers.

## End-to-End Scenarios

Test:

- Read profile
- Update profile
- Read settings
- Update settings
- Unauthorized access
- Forbidden access
- Invalid input
- Missing organization
- Cross-tenant access
- Concurrent update conflict
- Event publication
- Flyway startup from clean database

## Acceptance Criteria

- Architecture rules pass.
- End-to-end workflows pass.
- Full Maven verification succeeds.

---

# Phase 18: Final Hardening and Documentation Synchronization

## Goal

Prepare the module for merge and future extension.

## Tasks

Review:

- Package boundaries
- Naming consistency
- Enum consistency
- Permission consistency
- Error-code consistency
- Flyway consistency
- API documentation
- OpenAPI examples
- Logging safety
- Test reliability
- Unused code
- Duplicate abstractions
- TODO comments
- Documentation deviations

Run:

```bash
./mvnw clean verify
```

Also run configured checks such as:

```bash
./mvnw spotless:check
./mvnw checkstyle:check
```

## Deliverables

Provide:

- Files created
- Files modified
- Test results
- Migration details
- Architectural decisions
- Documentation deviations
- Deferred work
- Recommended next module integration point

## Acceptance Criteria

- Full build passes.
- All mandatory tests pass.
- No unresolved compilation warnings from new code.
- Documentation matches implementation.
- Deferred work is explicitly tracked.

---

# Recommended Codex Task Sequence

Create one Codex task for each phase.

Recommended first tasks:

```text
Task 0: Analyze existing OdinSync conventions
Task 1: Implement Organization value objects
Task 2: Implement Organization aggregate and domain events
Task 3: Define Organization application contracts
Task 4: Implement Organization application services
Task 5: Implement Organization JPA entities
Task 6: Add Organization Flyway schema
Task 7: Implement persistence mapper and repository adapter
Task 8: Implement Organization REST APIs
Task 9: Integrate Organization security
Task 10: Implement Organization error handling
Task 11: Add persistence integration tests
Task 12: Add controller and security tests
Task 13: Add tenant-isolation tests
Task 14: Add optimistic-locking tests
Task 15: Add domain-event publication
Task 16: Add observability
Task 17: Add architecture and end-to-end tests
Task 18: Final hardening and documentation sync
```

---

# Phase Dependency Map

```text
Phase 0
   │
   ▼
Phase 1
   │
   ▼
Phase 2
   │
   ▼
Phase 3
   │
   ▼
Phase 4
   │
   ├───────────────┐
   ▼               ▼
Phase 5         Phase 8
   │               │
   ▼               │
Phase 6            │
   │               │
   ▼               │
Phase 7 ◄───────────┘
   │
   ▼
Phase 9
   │
   ▼
Phase 10
   │
   ├───────────────┬───────────────┐
   ▼               ▼               ▼
Phase 11        Phase 12        Phase 15
   │               │               │
   └───────┬───────┘               │
           ▼                       │
        Phase 13                   │
           │                       │
           ▼                       │
        Phase 14                   │
           └───────────┬───────────┘
                       ▼
                    Phase 16
                       │
                       ▼
                    Phase 17
                       │
                       ▼
                    Phase 18
```

---
