# Organization Module

## Overview

The Organization module manages the business identity and global configuration of a tenant within OdinSync.

Every tenant has one organization that represents the company using the platform.

The Organization module is responsible for storing business information that is shared across all other business modules, such as:

- Company Name
- Legal Name
- Business Address
- Tax Information
- Currency
- Time Zone
- Locale
- Branding
- Business Preferences

The Organization module does **not** manage authentication or user credentials.

Authentication belongs to the Identity module.

The Organization module is the source of truth for Organization profile
persistence. Identity may initiate organization provisioning during registration,
but it must call the Organization application input port rather than persisting
Organization data directly.

---

# Purpose

The Organization module answers the business question:

> **Which company is using OdinSync and how is that company configured?**

It acts as the central source of truth for organization-wide settings consumed by all business modules.

Examples include:

- Invoice currency
- Default timezone
- Business name shown on invoices
- Tax registration number
- Company logo
- Regional settings

---

# Goals

The primary goals of the Organization module are:

- Maintain organization profile
- Store tenant-wide business settings
- Provide organization information to other modules
- Support localization
- Support branding
- Maintain business configuration independently of authentication

---

# Scope

The Organization module owns:

- Organization Profile
- Business Information
- Contact Information
- Address
- Currency
- Time Zone
- Locale
- Organization Status
- Business Preferences
- Branding

Current persistence ownership:

- The Organization module owns the Organization aggregate.
- The Organization module owns the `organizations` table mapping.
- The Organization module owns Organization repository adapters and persistence mappers.
- Identity may provision an Organization only through the Organization application input port.

Future ownership may include:

- Departments
- Branches
- Cost Centers
- Fiscal Years
- Holiday Calendars
- Business Units

---

# Out of Scope

The Organization module does NOT manage:

- Authentication
- Passwords
- Users
- Roles
- Permissions
- Customers
- Products
- Inventory
- Sales Orders
- Payments

These belong to their respective bounded contexts.

---

# Business Requirements

## BR-001

Every tenant must have exactly one organization.

Current implementation:

```
Tenant

↓

Organization
```

Relationship:

```
1 Tenant

↓

1 Organization
```

Future versions may support multiple organizations per tenant.

The design should avoid preventing this evolution.

Registration-time provisioning is initiated by Identity because registration
also creates credentials and the owner account. Organization creation itself is
executed by the Organization module through its provisioning use case, preserving
one local transaction inside the modular monolith.

---

## BR-002

Organization information must be editable.

Administrators should be able to update:

- Display Name
- Legal Name
- Address
- Contact Information
- Currency
- Time Zone
- Locale

---

## BR-003

Organization settings must be available throughout the platform.

Examples:

Finance

```
Currency
```

Sales

```
Timezone
```

Notification

```
Company Name
```

Invoice

```
Business Address
```

---

## BR-004

Organization information should be independent of authentication.

Updating business information must not require changes to:

- User Accounts
- JWT Tokens
- Sessions
- Roles

---

## BR-005

Every organization belongs to exactly one tenant.

Cross-tenant organization access is prohibited.

All queries must be scoped using:

```
tenantId
```

---

## BR-006

Organization updates should be auditable.

Every update should record:

- Updated By
- Updated At
- Previous Values (future enhancement)

---

# Functional Requirements

## FR-001

Retrieve organization profile.

---

## FR-002

Update organization profile.

---

## FR-003

Retrieve business settings.

---

## FR-004

Update business settings.

---

## FR-005

Upload company logo.

Future enhancement.

---

## FR-006

Enable or disable organization.

Future enhancement.

---

## FR-007

Retrieve supported currencies.

Future enhancement.

---

## FR-008

Retrieve supported locales.

Future enhancement.

---

# Non-Functional Requirements

## Performance

Organization lookup should normally complete in under:

```
100 ms
```

---

## Availability

Organization information is required by nearly every business module.

The module should have high availability.

---

## Scalability

The design should support:

- Millions of tenants
- Millions of organizations
- Future multi-region deployment

---

## Security

Only authorized administrators may modify organization information.

All requests must validate:

- JWT
- Tenant
- Role
- Permissions

---

## Auditability

Every modification should record:

- Created By
- Updated By
- Created At
- Updated At

Future versions may maintain full change history.

---

# Domain Model

The Organization context owns the following aggregate.

```
Organization
│
├── Business Information
├── Address
├── Contact Information
├── Branding
└── Business Settings
```

---

# Aggregate Root

The aggregate root is:

```
Organization
```

Every modification to organization data must pass through the Organization aggregate.

No external module may modify organization-owned entities directly.

---

# Aggregate Responsibilities

The Organization aggregate enforces business rules including:

- Valid business name
- Supported currency
- Supported locale
- Valid timezone
- Organization status
- Tenant ownership

---

# Ubiquitous Language

| Term | Meaning |
|------|---------|
| Organization | Company using OdinSync |
| Tenant | Security boundary |
| Legal Name | Registered company name |
| Display Name | User-friendly business name |
| Locale | Regional formatting preferences |
| Currency | Default business currency |
| Branding | Company logo and appearance |
| Business Settings | Organization-wide configuration |

---

# Relationship with Tenant

Identity owns:

```
Tenant
```

Organization owns:

```
Organization
```

Relationship:

```
Identity

Tenant

↓

Organization

Organization Profile
```

Tenant controls security.

Organization represents the business.

---

# Relationship with Other Modules

Organization provides configuration to:

```
CRM
```

Business Name

---

```
Sales
```

Currency

Timezone

---

```
Finance
```

Tax Information

Business Address

Currency

---

```
Notification
```

Company Name

Branding

---

The Organization module does not depend on any business module.

It only depends on Identity abstractions to determine:

- Current Tenant
- Current User
- Authorization

---

# Module Responsibilities

The Organization module is responsible for:

✅ Managing business profile

✅ Managing business settings

✅ Providing organization information

✅ Supporting localization

✅ Supporting branding

It is NOT responsible for:

❌ User Management

❌ Authentication

❌ Inventory

❌ Sales

❌ Financial Calculations

❌ Customer Management

---

# Success Criteria

The Organization module is considered complete when:

- Organization profile can be retrieved.
- Organization profile can be updated.
- Business settings are validated.
- Tenant isolation is enforced.
- Authorization is enforced.
- Organization data is available to other modules through public contracts.
- Audit fields are maintained.
- All module tests pass.

---

# Aggregate Design

## Overview

The Organization module follows the Domain-Driven Design (DDD) aggregate pattern.

The aggregate protects business consistency by ensuring all modifications to organization data pass through a single entry point.

The aggregate root is:

```
Organization
```

No external module should directly modify Organization-owned entities.

---

# Aggregate Structure

```
Organization (Aggregate Root)
│
├── OrganizationAddress
├── OrganizationContact
├── OrganizationSettings
└── OrganizationBranding (Future)
```

Future versions may introduce additional entities such as:

```
Organization
│
├── Departments
├── Branches
├── BusinessUnits
├── FiscalYears
└── HolidayCalendars
```

Those should become independent aggregates only when their lifecycle becomes sufficiently complex.

---

# Aggregate Responsibilities

The Organization aggregate is responsible for enforcing:

- Organization always belongs to one tenant
- Legal name cannot be empty
- Display name cannot be empty
- Currency must be supported
- Time zone must be valid
- Locale must be valid
- Organization status transitions
- Audit information

No repository should bypass these rules.

---

# Aggregate Lifecycle

```
Organization Registered

        │

        ▼

Active

        │

        ▼

Updated

        │

        ▼

Archived (Future)
```

Organization deletion is intentionally not supported.

Historical business information should remain available.

---

# Entity Design

The Organization aggregate consists of several entities.

```
Organization
│
├── OrganizationAddress
├── OrganizationContact
└── OrganizationSettings
```

---

# Organization Entity

The aggregate root.

## Responsibilities

Owns:

- Organization identity
- Tenant relationship
- Business information
- Audit fields
- Aggregate invariants

---

## Attributes

| Field | Description |
|--------|-------------|
| id | Organization identifier |
| tenantId | Tenant owner |
| legalName | Registered company name |
| displayName | Business display name |
| taxRegistrationNumber | GST / VAT identifier |
| status | Current organization status |
| address | Business address |
| contact | Contact information |
| settings | Business settings |
| createdAt | Creation timestamp |
| updatedAt | Last modification timestamp |
| createdBy | Creator |
| updatedBy | Last updater |

---

## Identity

```
OrganizationId
```

The identifier never changes.

---

## Invariants

The aggregate guarantees:

- tenantId cannot change
- legalName cannot be empty
- displayName cannot be empty
- address must exist
- settings must exist
- contact must exist

---

# OrganizationAddress Entity

Represents the business address.

## Responsibilities

Stores:

- Street
- City
- State
- Postal Code
- Country

---

## Attributes

| Field | Description |
|--------|-------------|
| line1 | Address line 1 |
| line2 | Address line 2 |
| city | City |
| state | State |
| postalCode | ZIP / PIN |
| country | Country |

---

## Validation

- line1 required
- city required
- state required
- country required
- postal code format validated

---

# OrganizationContact Entity

Represents contact information.

## Attributes

| Field | Description |
|--------|-------------|
| email | Business email |
| phone | Business phone |
| website | Company website |

---

## Validation

Email

- valid format

Phone

- valid international format

Website

- optional
- valid URL

---

# OrganizationSettings Entity

Represents organization-wide configuration.

---

## Responsibilities

Stores configuration consumed by other modules.

Examples:

Finance

```
Currency
```

Sales

```
Timezone
```

Notification

```
Locale
```

---

## Attributes

| Field | Description |
|--------|-------------|
| currency | Default currency |
| timezone | Default timezone |
| locale | Locale |
| dateFormat | Preferred date format |
| timeFormat | 12/24 hour |

Future additions:

- Number format
- Fiscal year
- Working days
- Week start
- Default tax profile

---

# OrganizationBranding (Future)

Branding is intentionally separated.

Future fields:

- Company Logo
- Theme Color
- Email Footer
- Invoice Logo

This keeps branding independent from business settings.

---

# Value Objects

Several concepts should be modeled as Value Objects.

---

## OrganizationName

Encapsulates:

```
Legal Name

Display Name
```

Responsibilities:

- trim whitespace
- validate length
- prevent blank values

---

## Address

Represents an immutable address.

```
Street

City

State

Postal Code

Country
```

Advantages:

- immutable
- reusable
- self-validating

---

## EmailAddress

Encapsulates:

- email validation
- normalization
- formatting

Instead of:

```
String email
```

Prefer:

```
EmailAddress
```

---

## PhoneNumber

Encapsulates:

- formatting
- validation
- normalization

---

## Currency

Represents ISO-4217 currencies.

Examples:

```
INR

USD

EUR
```

Prefer enum or dedicated value object.

---

## TimeZone

Represents valid IANA time zones.

Examples:

```
Asia/Kolkata

Australia/Melbourne

Europe/London
```

Avoid arbitrary strings.

---

## Locale

Examples:

```
en_IN

en_US

en_AU
```

Determines:

- number formatting
- date formatting
- language

---

# Domain Services

Most business rules belong inside the aggregate.

Domain services should only exist when logic spans multiple entities.

Current Organization module requires very few domain services.

Possible future services:

```
OrganizationValidationService

BusinessCalendarService

HolidayCalculationService
```

Currently unnecessary.

---

# Repository Interface

The Organization aggregate should be persisted through a repository abstraction.

Example:

```java
public interface OrganizationRepository {

    Optional<Organization> findByTenantId(
            TenantId tenantId);

    Organization save(
            Organization organization);

}
```

Repository belongs to:

```
application.port.out
```

Implementation belongs to:

```
infrastructure.persistence
```

---

# Repository Rules

Repositories should never expose:

```
saveAddress()

saveSettings()

saveContact()
```

The aggregate is saved as a whole.

---

# Package Structure

Recommended package layout:

```
organization

├── domain
│   ├── model
│   │   ├── Organization
│   │   ├── OrganizationAddress
│   │   ├── OrganizationContact
│   │   ├── OrganizationSettings
│   │   └── valueobject
│   │
│   ├── event
│   └── service
│
├── application
│   ├── dto
│   ├── mapper
│   ├── port
│   │   ├── in
│   │   └── out
│   └── usecase
│
├── infrastructure
│   ├── persistence
│   ├── configuration
│   └── mapper
│
└── presentation
    ├── rest
    └── dto
```

---

# Aggregate Consistency Rules

The aggregate guarantees:

✓ Every organization belongs to one tenant.

✓ Every organization has one settings object.

✓ Every organization has one address.

✓ Every organization has one contact.

✓ Every organization has a legal name.

✓ Every organization has a display name.

---

# Summary

The Organization aggregate acts as the single consistency boundary for all organization-owned business data.

Its responsibilities are intentionally limited to business identity and global configuration.

The use of entities, value objects, and aggregate rules keeps business logic cohesive while preventing invalid state changes.

No external module should modify organization data except through the aggregate's public behavior.


---

# Application Layer Design

## Overview

The application layer coordinates Organization use cases.

It receives requests from the presentation layer, resolves the authenticated tenant and actor, loads the Organization aggregate, invokes domain behavior, persists changes, and returns application DTOs.

The application layer must not contain persistence-specific logic or HTTP-specific logic.

Its responsibilities are:

- Coordinate use cases
- Enforce application-level authorization
- Establish tenant scope
- Load and save aggregates
- Manage transaction boundaries
- Publish domain events
- Map domain models to application responses

The application layer depends on:

- Organization domain model
- Repository ports
- Identity public contracts
- Event publishing ports

It must not depend directly on:

- Spring MVC controllers
- JPA entities
- Database repositories
- HTTP request objects
- External SDK implementations

---

# Primary Use Cases

The initial Organization module supports four primary use cases:

## 1. Get Organization Profile

Retrieves the organization profile associated with the authenticated tenant.

The use case returns:

- Organization identity
- Legal name
- Display name
- Tax registration number
- Address
- Contact information
- Organization status
- Audit metadata

The use case must never accept `tenantId` directly from an external client.

The tenant must be resolved from the authenticated security context.

---

## 2. Update Organization Profile

Updates the core business information of the current organization.

Editable fields include:

- Legal name
- Display name
- Tax registration number
- Address
- Contact email
- Contact phone
- Website

The use case must:

1. Resolve the authenticated tenant.
2. Verify the actor has permission to update the organization.
3. Load the organization using the authenticated tenant.
4. Invoke aggregate behavior.
5. Persist the aggregate.
6. Publish resulting domain events.
7. Return the updated profile.

---

## 3. Get Organization Settings

Retrieves tenant-wide business configuration for the authenticated organization.

The use case returns:

- Default currency
- Time zone
- Locale
- Date format
- Time format
- Week start preference
- Other supported organization-level settings

The settings are consumed by modules such as:

- Sales
- Finance
- Notification
- Reporting

The use case must:

1. Resolve the authenticated tenant.
2. Load the organization using the tenant identifier.
3. Retrieve the settings from the aggregate.
4. Map the settings to an application response.
5. Return the response.

Read access may be available to authenticated users who require organization settings for platform operation.

Modification access remains restricted.

---

## 4. Update Organization Settings

Updates tenant-wide configuration.

Editable settings initially include:

- Currency
- Time zone
- Locale
- Date format
- Time format

The use case must:

1. Resolve the authenticated tenant and actor.
2. Verify that the actor has permission to manage organization settings.
3. Load the organization.
4. Validate the requested settings.
5. Invoke aggregate behavior.
6. Persist the updated aggregate.
7. Publish domain events.
8. Return the updated settings.

Changes to organization settings can affect multiple modules.

For example:

```text
CurrencyChanged
        │
        ├── Finance uses the new default for future invoices
        └── Sales uses the new default for future quotations
```

Existing transactional records must not be retroactively modified.

For example, changing the default currency from `INR` to `USD` must not change the currency of existing orders or invoices.

---

# Use Case Interfaces

Inbound ports define the application use cases exposed by the Organization module.

The presentation layer (REST, GraphQL, gRPC, CLI, etc.) communicates only with these interfaces.

---

## OrganizationQueryUseCase

Responsible for read operations.

```java
public interface OrganizationQueryUseCase {

    OrganizationResponse getOrganization();

    OrganizationSettingsResponse getSettings();

}
```

---

## OrganizationCommandUseCase

Responsible for state-changing operations.

```java
public interface OrganizationCommandUseCase {

    OrganizationResponse updateOrganization(
            UpdateOrganizationCommand command);

    OrganizationSettingsResponse updateSettings(
            UpdateOrganizationSettingsCommand command);

}
```

---

# Commands

Commands represent business intentions.

Commands should:

- be immutable
- contain validated input
- not contain business logic
- not contain persistence logic

---

## UpdateOrganizationCommand

```java
public record UpdateOrganizationCommand(

        String legalName,

        String displayName,

        String taxRegistrationNumber,

        AddressCommand address,

        ContactCommand contact

) {
}
```

---

## AddressCommand

```java
public record AddressCommand(

        String line1,

        String line2,

        String city,

        String state,

        String postalCode,

        String country

) {
}
```

---

## ContactCommand

```java
public record ContactCommand(

        String email,

        String phone,

        String website

) {
}
```

---

## UpdateOrganizationSettingsCommand

```java
public record UpdateOrganizationSettingsCommand(

        String currency,

        String timezone,

        String locale,

        String dateFormat,

        String timeFormat

) {
}
```

---

# Queries

Unlike commands, queries never modify state.

They only retrieve information.

Initial queries include:

- Get Organization
- Get Organization Settings

Future queries may include:

- Get Organization Branding
- Get Fiscal Year
- Get Business Units
- Get Branches

---

# Response DTOs

Application responses should expose only information required by clients.

They must never expose:

- JPA entities
- Aggregate internals
- Internal identifiers
- Lazy-loaded objects

---

## OrganizationResponse

```java
public record OrganizationResponse(

        UUID id,

        String legalName,

        String displayName,

        String taxRegistrationNumber,

        AddressResponse address,

        ContactResponse contact,

        OrganizationStatus status,

        Instant createdAt,

        Instant updatedAt

) {
}
```

---

## AddressResponse

```java
public record AddressResponse(

        String line1,

        String line2,

        String city,

        String state,

        String postalCode,

        String country

) {
}
```

---

## ContactResponse

```java
public record ContactResponse(

        String email,

        String phone,

        String website

) {
}
```

---

## OrganizationSettingsResponse

```java
public record OrganizationSettingsResponse(

        String currency,

        String timezone,

        String locale,

        String dateFormat,

        String timeFormat

) {
}
```

---

# REST API Design

The Organization module exposes a REST API under a versioned endpoint.

```
/api/v1/organizations
```

All endpoints require authentication.

The authenticated tenant determines which organization is accessed.

---

## Get Organization

```
GET /api/v1/organizations/me
```

Purpose:

Retrieve the current tenant's organization profile.

Authorization:

```
organization:read
```

Response:

```
200 OK
```

Returns:

```json
{
  "id": "...",
  "legalName": "Acme Private Limited",
  "displayName": "Acme",
  "status": "ACTIVE"
}
```

---

## Update Organization

```
PUT /api/v1/organizations/me
```

Authorization:

```
organization:update
```

Request:

```json
{
  "legalName": "Acme Private Limited",
  "displayName": "Acme",
  "taxRegistrationNumber": "GST12345",
  "address": { ... },
  "contact": { ... }
}
```

Responses:

```
200 OK
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
422 Unprocessable Entity
```

---

## Get Organization Settings

```
GET /api/v1/organizations/me/settings
```

Authorization:

```
organization:settings:read
```

Response:

```
200 OK
```

---

## Update Organization Settings

```
PUT /api/v1/organizations/me/settings
```

Authorization:

```
organization:settings:update
```

Example request:

```json
{
  "currency": "INR",
  "timezone": "Asia/Kolkata",
  "locale": "en_IN",
  "dateFormat": "dd/MM/yyyy",
  "timeFormat": "24H"
}
```

---

# Sequence Diagram — Update Organization

```text
Client
   │
   ▼
OrganizationController
   │
   ▼
OrganizationCommandUseCase
   │
   ▼
Resolve Current Tenant
   │
   ▼
OrganizationRepository
   │
   ▼
Load Organization Aggregate
   │
   ▼
Organization.updateProfile(...)
   │
   ▼
Business Rule Validation
   │
   ▼
OrganizationRepository.save(...)
   │
   ▼
Publish Domain Events
   │
   ▼
Return Response
```

---

# Transaction Boundary

Each command executes within a single transaction.

```
BEGIN TRANSACTION

Load Aggregate

↓

Validate

↓

Execute Domain Logic

↓

Persist Aggregate

↓

Publish Domain Events

↓

COMMIT
```

If any step fails:

- transaction is rolled back
- no partial updates are persisted
- no domain events are published

---

# Mapping Responsibilities

| Layer | Responsibility |
|--------|----------------|
| Controller | HTTP ↔ DTO conversion |
| Use Case | Business orchestration |
| Domain | Business rules |
| Repository | Persistence |
| Mapper | Domain ↔ DTO conversion |

Business rules must never be implemented inside controllers or mappers.

---

# Application Layer Principles

The Organization application layer should remain:

- Thin
- Transactional
- Stateless
- Testable
- Independent of frameworks

Its role is orchestration—not business decision making.

The Organization aggregate remains the single source of business behavior and invariants.

---

# Persistence and Infrastructure Design

## Overview

The infrastructure layer implements the technical capabilities required by the Organization module.

Its responsibilities include:

- Persisting Organization aggregates
- Mapping domain objects to persistence models
- Implementing repository ports
- Enforcing database constraints
- Publishing integration events
- Adapting the authenticated security context
- Translating infrastructure failures into application exceptions

The infrastructure layer may depend on:

- Spring Data JPA
- Hibernate
- Flyway
- Spring Security
- Spring Application Events
- Kafka or another message broker in future versions

The domain and application layers must not depend on these infrastructure technologies.

---

# Persistence Strategy

The Organization aggregate is stored in a relational database.

The initial OdinSync deployment uses one shared database for all tenants.

Tenant isolation is enforced through:

```text
tenant_id
```

Every Organization query must include the tenant identifier.

The database structure should support future migration to:

- Separate schemas per bounded context
- Separate databases per service
- Dedicated databases for premium tenants
- Read replicas
- Multi-region deployment

---

# Aggregate Persistence Model

The initial implementation stores the Organization aggregate using the following tables:

```text
organizations
organization_addresses
organization_contacts
organization_settings
```

Conceptual relationship:

```text
organizations
    │
    ├── 1:1 organization_addresses
    ├── 1:1 organization_contacts
    └── 1:1 organization_settings
```

Each dependent table uses `organization_id` as its foreign key.

The aggregate must be loaded and saved as one consistency boundary.

---

# Organization JPA Entity

The persistence entity represents how Organization data is stored.

It must not be used as the domain model.

Recommended package:

```text
organization.infrastructure.persistence.entity
```

Example:

```java
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_organizations_tenant_id",
                        columnNames = "tenant_id")
        },
        indexes = {
                @Index(
                        name = "idx_organizations_tenant_id",
                        columnList = "tenant_id"),
                @Index(
                        name = "idx_organizations_status",
                        columnList = "status")
        }
)
public class OrganizationJpaEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "tenant_id",
            nullable = false,
            updatable = false
    )
    private UUID tenantId;

    @Column(
            name = "legal_name",
            nullable = false,
            length = 200
    )
    private String legalName;

    @Column(
            name = "display_name",
            nullable = false,
            length = 120
    )
    private String displayName;

    @Column(
            name = "tax_registration_number",
            length = 50
    )
    private String taxRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private OrganizationStatusJpa status;

    @OneToOne(
            mappedBy = "organization",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY,
            optional = false
    )
    private OrganizationAddressJpaEntity address;

    @OneToOne(
            mappedBy = "organization",

            cascade = CascadeType.ALL,

            orphanRemoval = true,

            fetch = FetchType.LAZY,

            optional = false

    )

    private OrganizationContactJpaEntity contact;

    @OneToOne(

            mappedBy = "organization",

            cascade = CascadeType.ALL,

            orphanRemoval = true,

            fetch = FetchType.LAZY,

            optional = false

    )

    private OrganizationSettingsJpaEntity settings;

    @Version

    @Column(

            name = "version",

            nullable = false

    )

    private long version;

    @Column(

            name = "created_at",

            nullable = false,

            updatable = false

    )

    private Instant createdAt;

    @Column(

            name = "updated_at",

            nullable = false

    )

    private Instant updatedAt;

    @Column(

            name = "created_by",

            nullable = false,

            updatable = false

    )

    private UUID createdBy;

    @Column(

            name = "updated_by",

            nullable = false

    )

    private UUID updatedBy;

}

```

---

# Design Decisions

Several design decisions are reflected in this entity.

## UUID Primary Keys

Every aggregate uses UUID identifiers.

Advantages:

- Globally unique

- Easier future microservice migration

- Safe across distributed systems

- No database sequence coordination

---

## Tenant ID

```java

private UUID tenantId;

```

The tenant identifier is immutable.

Once an organization is created it can never belong to another tenant.

Therefore:

```java

updatable = false

```

is intentional.

---

## Optimistic Locking

```java

@Version

private long version;

```

This prevents lost updates.

Example:

Administrator A

↓

Version = 5

Administrator B

↓

Version = 5

Administrator A saves

↓

Version = 6

Administrator B saves

↓

OptimisticLockException

↓

409 Conflict

---

## Cascade Strategy

The aggregate owns:

- Address

- Contact

- Settings

Therefore:

```java

CascadeType.ALL

```

is appropriate.

Saving Organization automatically persists owned entities.

Deleting Organization removes owned entities.

---

## Orphan Removal

```java

orphanRemoval = true

```

If an owned entity is replaced:

Old Address

↓

removed automatically

No orphan rows remain.

---

## Lazy Loading

```java

FetchType.LAZY

```

Recommended.

Reasons:

- Avoid unnecessary joins

- Better scalability

- Faster list queries

The aggregate mapper controls when related entities are loaded.

---

# OrganizationAddressJpaEntity

```java

@Entity

@Table(name = "organization_addresses")

public class OrganizationAddressJpaEntity {

    @Id

    @Column(name = "organization_id")

    private UUID organizationId;

    @MapsId

    @OneToOne(fetch = FetchType.LAZY)

    @JoinColumn(

            name = "organization_id",

            nullable = false

    )

    private OrganizationJpaEntity organization;

    @Column(name = "line1", nullable = false, length = 200)

    private String line1;

    @Column(name = "line2", length = 200)

    private String line2;

    @Column(name = "city", nullable = false, length = 100)

    private String city;

    @Column(name = "state", nullable = false, length = 100)

    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)

    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)

    private String countryCode;

}

```

---

# Why @MapsId?

The address has no identity outside the Organization aggregate.

Instead of:

```

organization_address.id

organization_id

```

we use:

```

organization_id

```

as both:

- Primary Key

- Foreign Key

Advantages:

- Simpler schema

- Enforces true 1:1 ownership

- Prevents duplicate address rows

The same pattern should be used for:

- OrganizationContactJpaEntity

- OrganizationSettingsJpaEntity

---

# OrganizationContactJpaEntity

```java

@Entity

@Table(name = "organization_contacts")

public class OrganizationContactJpaEntity {

    @Id

    @Column(name = "organization_id")

    private UUID organizationId;

    @MapsId

    @OneToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "organization_id")

    private OrganizationJpaEntity organization;

    @Column(name = "email", nullable = false, length = 254)

    private String email;

    @Column(name = "phone", nullable = false, length = 30)

    private String phone;

    @Column(name = "website", length = 500)

    private String website;

}

```

---

# OrganizationSettingsJpaEntity

```java

@Entity

@Table(name = "organization_settings")

public class OrganizationSettingsJpaEntity {

    @Id

    @Column(name = "organization_id")

    private UUID organizationId;

    @MapsId

    @OneToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "organization_id")

    private OrganizationJpaEntity organization;

    @Column(name = "currency_code", nullable = false, length = 3)

    private String currencyCode;

    @Column(name = "time_zone", nullable = false, length = 100)

    private String timeZone;

    @Column(name = "locale", nullable = false, length = 20)

    private String locale;

    @Column(name = "date_format", nullable = false, length = 30)

    private String dateFormat;

    @Column(name = "time_format", nullable = false, length = 20)

    private String timeFormat;

}

```

---

# Why Separate Tables?

Keeping Address, Contact, and Settings in separate tables has several advantages.

## Better Separation of Concerns

Each entity manages a distinct business concept.

## Smaller Rows

Frequently updated settings do not rewrite address columns.

## Future Evolution

Settings may later become versioned or independently audited without redesigning the Organization table.

## Aggregate Ownership Preserved

Although stored separately, these tables remain part of the same aggregate and transaction boundary.

They must never be modified independently of the Organization aggregate.

---

# Persistence Rules

The infrastructure model must follow these rules:

- JPA entities remain in the infrastructure layer.

- Domain objects must never contain JPA annotations.

- Mapping between domain and persistence is handled by dedicated mappers.

- Repository implementations work with JPA entities internally and expose only domain objects through repository ports.

- All persistence operations remain tenant-scoped.

# Persistence Mapper

The persistence mapper converts between:

```text
Organization Domain Aggregate
            ↕
Organization JPA Entity Graph
```

Recommended package:

```text
organization.infrastructure.persistence.mapper
```

The mapper must not contain business rules.

Its responsibilities are limited to:

- Reconstructing a domain aggregate from persisted state
- Converting a domain aggregate into persistence entities
- Maintaining bidirectional JPA relationships
- Preserving identifiers, versions, and audit metadata

---

## Mapper Contract

```java
public interface OrganizationPersistenceMapper {

    Organization toDomain(
            OrganizationJpaEntity entity);

    OrganizationJpaEntity toEntity(
            Organization organization);
}
```

A class-based mapper is preferable when aggregate reconstruction requires explicit factory methods.

```java
@Component
public class OrganizationPersistenceMapperImpl
        implements OrganizationPersistenceMapper {

    @Override
    public Organization toDomain(
            OrganizationJpaEntity entity) {

        return Organization.reconstitute(
                OrganizationId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                OrganizationName.of(
                        entity.getLegalName(),
                        entity.getDisplayName()),
                TaxRegistrationNumber.optional(
                        entity.getTaxRegistrationNumber()),
                mapAddress(entity.getAddress()),
                mapContact(entity.getContact()),
                mapSettings(entity.getSettings()),
                mapStatus(entity.getStatus()),
                entity.getVersion(),
                AuditMetadata.reconstitute(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        UserId.of(entity.getCreatedBy()),
                        UserId.of(entity.getUpdatedBy())));
    }

    @Override
    public OrganizationJpaEntity toEntity(
            Organization organization) {

        OrganizationJpaEntity entity =
                new OrganizationJpaEntity();

        entity.setId(organization.id().value());
        entity.setTenantId(
                organization.tenantId().value());
        entity.setLegalName(
                organization.name().legalName());
        entity.setDisplayName(
                organization.name().displayName());
        entity.setTaxRegistrationNumber(
                organization.taxRegistrationNumber()
                        .map(TaxRegistrationNumber::value)
                        .orElse(null));
        entity.setStatus(
                OrganizationStatusJpa.from(
                        organization.status()));
        entity.setVersion(
                organization.version());
        entity.setCreatedAt(
                organization.auditMetadata()
                        .createdAt());
        entity.setUpdatedAt(
                organization.auditMetadata()
                        .updatedAt());
        entity.setCreatedBy(
                organization.auditMetadata()
                        .createdBy()
                        .value());
        entity.setUpdatedBy(
                organization.auditMetadata()
                        .updatedBy()
                        .value());

        OrganizationAddressJpaEntity address =
                mapAddress(organization.address());

        OrganizationContactJpaEntity contact =
                mapContact(organization.contact());

        OrganizationSettingsJpaEntity settings =
                mapSettings(organization.settings());

        entity.replaceAddress(address);
        entity.replaceContact(contact);
        entity.replaceSettings(settings);

        return entity;
    }
}
```

---

# Aggregate Reconstitution

Loading an aggregate from persistence is different from creating a new aggregate.

New aggregate creation may:

- Validate creation rules
- Generate identifiers
- Set initial status
- Record domain events
- Assign creation timestamps

Reconstitution must:

- Restore existing state
- Preserve persisted identifiers
- Preserve audit metadata
- Preserve optimistic-lock version
- Avoid recording new domain events

Recommended factory:

```java
public static Organization reconstitute(
        OrganizationId id,
        TenantId tenantId,
        OrganizationName name,
        TaxRegistrationNumber taxRegistrationNumber,
        Address address,
        OrganizationContact contact,
        OrganizationSettings settings,
        OrganizationStatus status,
        long version,
        AuditMetadata auditMetadata
) {
    return new Organization(
            id,
            tenantId,
            name,
            taxRegistrationNumber,
            address,
            contact,
            settings,
            status,
            version,
            auditMetadata,
            new ArrayList<>());
}
```

Do not call a public creation method such as:

```java
Organization.create(...)
```

while loading persisted data.

Doing so may incorrectly generate:

- New IDs
- New timestamps
- Creation events
- Initial default values

---

# Maintaining Bidirectional Relationships

JPA relationships must be synchronized on both sides.

The root persistence entity should provide helper methods.

```java
public void replaceAddress(
        OrganizationAddressJpaEntity address) {

    this.address = address;

    if (address != null) {
        address.setOrganization(this);
    }
}
```

```java
public void replaceContact(
        OrganizationContactJpaEntity contact) {

    this.contact = contact;

    if (contact != null) {
        contact.setOrganization(this);
    }
}
```

```java
public void replaceSettings(
        OrganizationSettingsJpaEntity settings) {

    this.settings = settings;

    if (settings != null) {
        settings.setOrganization(this);
    }
}
```

These methods prevent inconsistent entity graphs.

Avoid assigning owned entities directly from infrastructure services.

---

# Spring Data Repository

Spring Data provides database access for the JPA entity.

Recommended package:

```text
organization.infrastructure.persistence.repository
```

```java
public interface SpringDataOrganizationRepository
        extends JpaRepository<
                OrganizationJpaEntity,
                UUID> {

    @EntityGraph(attributePaths = {
            "address",
            "contact",
            "settings"
    })
    Optional<OrganizationJpaEntity>
            findByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {
            "address",
            "contact",
            "settings"
    })
    Optional<OrganizationJpaEntity>
            findByIdAndTenantId(
                    UUID id,
                    UUID tenantId);

    boolean existsByTenantId(UUID tenantId);
}
```

---

# Why Use EntityGraph?

The relationships are configured as lazy.

However, the Organization aggregate requires:

- Address
- Contact
- Settings

when it is reconstructed.

Using:

```java
@EntityGraph
```

loads the aggregate graph intentionally for aggregate operations.

This avoids:

- `LazyInitializationException`
- Multiple follow-up queries
- Accidental N+1 query patterns
- Changing every relationship to eager loading

The default relationship fetch mode should remain lazy.

The repository query determines when the full graph is needed.

---

# Repository Adapter

The repository adapter implements the application-layer output port.

```java
@Repository
public class OrganizationRepositoryAdapter
        implements OrganizationRepository {

    private final SpringDataOrganizationRepository
            springDataRepository;

    private final OrganizationPersistenceMapper
            persistenceMapper;

    public OrganizationRepositoryAdapter(
            SpringDataOrganizationRepository
                    springDataRepository,
            OrganizationPersistenceMapper
                    persistenceMapper
    ) {
        this.springDataRepository =
                springDataRepository;
        this.persistenceMapper =
                persistenceMapper;
    }

    @Override
    public Optional<Organization> findByTenantId(
            TenantId tenantId
    ) {
        return springDataRepository
                .findByTenantId(tenantId.value())
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<Organization> findByIdAndTenantId(
            OrganizationId organizationId,
            TenantId tenantId
    ) {
        return springDataRepository
                .findByIdAndTenantId(
                        organizationId.value(),
                        tenantId.value())
                .map(persistenceMapper::toDomain);
    }

    @Override
    public boolean existsByTenantId(
            TenantId tenantId
    ) {
        return springDataRepository
                .existsByTenantId(
                        tenantId.value());
    }

    @Override
    public Organization save(
            Organization organization
    ) {
        OrganizationJpaEntity entity =
                persistenceMapper.toEntity(
                        organization);

        OrganizationJpaEntity saved =
                springDataRepository.save(entity);

        return persistenceMapper.toDomain(saved);
    }
}
```

---

# Updating Existing JPA Entities

Creating a new JPA entity graph for every update is simple, but it may cause problems with:

- Detached entity handling
- Unnecessary inserts
- Optimistic locking
- Orphan removal
- Hibernate-managed state

For updates, the preferred approach is to load the managed entity and apply domain state to it.

Recommended adapter flow:

```java
@Override
public Organization save(
        Organization organization
) {
    OrganizationJpaEntity entity =
            springDataRepository
                    .findByIdAndTenantId(
                            organization.id().value(),
                            organization.tenantId().value())
                    .orElseGet(
                            () -> persistenceMapper
                                    .toEntity(
                                            organization));

    persistenceMapper.updateEntity(
            organization,
            entity);

    OrganizationJpaEntity saved =
            springDataRepository.save(entity);

    return persistenceMapper.toDomain(saved);
}
```

Mapper update method:

```java
void updateEntity(
        Organization source,
        OrganizationJpaEntity target);
```

This method updates fields on a managed persistence graph.

It must not replace immutable fields such as:

- Organization ID
- Tenant ID
- Created timestamp
- Created-by user

---

# Create and Update Separation

For maximum clarity, the repository adapter may distinguish between new and existing aggregates.

```java
if (organization.isNew()) {
    return insert(organization);
}

return update(organization);
```

Possible domain method:

```java
public boolean isNew() {
    return version == 0;
}
```

However, version `0` may also represent a persisted row depending on JPA configuration.

A clearer option is to track persistence state explicitly or use repository existence checks during implementation.

The initial implementation may use:

```java
springDataRepository.existsById(
        organization.id().value())
```

before deciding between insert and update.

---

# Database Schema

The Organization aggregate uses four tables.

```text
organizations
organization_addresses
organization_contacts
organization_settings
```

---

## organizations

| Column | Type | Constraints |
|---|---|---|
| `id` | `BINARY(16)` | Primary key |
| `tenant_id` | `BINARY(16)` | Not null, unique |
| `legal_name` | `VARCHAR(200)` | Not null |
| `display_name` | `VARCHAR(120)` | Not null |
| `tax_registration_number` | `VARCHAR(50)` | Nullable |
| `status` | `VARCHAR(30)` | Not null |
| `version` | `BIGINT` | Not null |
| `created_at` | `TIMESTAMP(6)` | Not null |
| `updated_at` | `TIMESTAMP(6)` | Not null |
| `created_by` | `BINARY(16)` | Not null |
| `updated_by` | `BINARY(16)` | Not null |

For MySQL, UUID values may be stored as:

```text
BINARY(16)
```

This is more compact and index-efficient than:

```text
CHAR(36)
```

However, it requires consistent UUID conversion configuration.

For the first implementation, `CHAR(36)` may be selected for simpler debugging.

The project must choose one format consistently across all modules.

---

## organization_addresses

| Column | Type | Constraints |
|---|---|---|
| `organization_id` | `BINARY(16)` | Primary key, foreign key |
| `line1` | `VARCHAR(200)` | Not null |
| `line2` | `VARCHAR(200)` | Nullable |
| `city` | `VARCHAR(100)` | Not null |
| `state` | `VARCHAR(100)` | Not null |
| `postal_code` | `VARCHAR(20)` | Not null |
| `country_code` | `CHAR(2)` | Not null |

---

## organization_contacts

| Column | Type | Constraints |
|---|---|---|
| `organization_id` | `BINARY(16)` | Primary key, foreign key |
| `email` | `VARCHAR(254)` | Not null |
| `phone` | `VARCHAR(30)` | Not null |
| `website` | `VARCHAR(500)` | Nullable |

---

## organization_settings

| Column | Type | Constraints |
|---|---|---|
| `organization_id` | `BINARY(16)` | Primary key, foreign key |
| `currency_code` | `CHAR(3)` | Not null |
| `time_zone` | `VARCHAR(100)` | Not null |
| `locale` | `VARCHAR(20)` | Not null |
| `date_format` | `VARCHAR(30)` | Not null |
| `time_format` | `VARCHAR(20)` | Not null |
| `week_start_day` | `VARCHAR(15)` | Not null |

The week-start setting should be included if it is part of the application response.

Recommended values:

```text
MONDAY
SUNDAY
SATURDAY
```

Prefer an enum-like application value rather than a numeric day unless numeric semantics are clearly documented.

---

# Foreign Key Strategy

Dependent tables reference the root organization row.

```text
organization_addresses.organization_id
    → organizations.id

organization_contacts.organization_id
    → organizations.id

organization_settings.organization_id
    → organizations.id
```

Recommended delete behavior:

```sql
ON DELETE CASCADE
```

This ensures aggregate-owned rows are removed if the aggregate root is deleted.

Deletion must still occur only through a controlled Organization use case.

Direct database deletion should not be used as an application workflow.

---

# Flyway Migration

Recommended migration file:

```text
src/main/resources/db/migration/
V4__create_organization_tables.sql
```

The migration number must follow the current OdinSync migration sequence.

Example using MySQL `BINARY(16)` UUID storage:

```sql
CREATE TABLE organizations (
    id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    tax_registration_number VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,

    CONSTRAINT pk_organizations
        PRIMARY KEY (id),

    CONSTRAINT uk_organizations_tenant_id
        UNIQUE (tenant_id),

    CONSTRAINT chk_organizations_status
        CHECK (
            status IN (
                'ACTIVE',
                'SUSPENDED',
                'ARCHIVED'
            )
        )
);

CREATE INDEX idx_organizations_status
    ON organizations(status);
```

The unique constraint on `tenant_id` already creates a supporting index in MySQL.

Creating both:

```text
UNIQUE (tenant_id)
```

and:

```text
INDEX (tenant_id)
```

is usually redundant.

Therefore, the JPA declaration should avoid creating a separate duplicate tenant index.

---

## Address Table Migration

```sql
CREATE TABLE organization_addresses (
    organization_id BINARY(16) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    line2 VARCHAR(200) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country_code CHAR(2) NOT NULL,

    CONSTRAINT pk_organization_addresses
        PRIMARY KEY (organization_id),

    CONSTRAINT fk_org_addresses_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_org_addresses_country_code
        CHECK (
            country_code REGEXP '^[A-Z]{2}$'
        )
);
```

Database regular-expression checks may differ between database vendors.

If portability is important, enforce only length and non-null constraints in the database and keep format validation in the domain layer.

---

## Contact Table Migration

```sql
CREATE TABLE organization_contacts (
    organization_id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    website VARCHAR(500) NULL,

    CONSTRAINT pk_organization_contacts
        PRIMARY KEY (organization_id),

    CONSTRAINT fk_org_contacts_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);
```

Do not add a unique constraint to the contact email unless the business explicitly requires one email to belong to only one organization.

Shared business emails such as:

```text
accounts@example.com
```

may legitimately be reused.

---

## Settings Table Migration

```sql
CREATE TABLE organization_settings (
    organization_id BINARY(16) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    date_format VARCHAR(30) NOT NULL,
    time_format VARCHAR(20) NOT NULL,
    week_start_day VARCHAR(15) NOT NULL,

    CONSTRAINT pk_organization_settings
        PRIMARY KEY (organization_id),

    CONSTRAINT fk_org_settings_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_org_settings_currency
        CHECK (
            currency_code REGEXP '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_org_settings_time_format
        CHECK (
            time_format IN (
                '12_HOUR',
                '24_HOUR'
            )
        ),

    CONSTRAINT chk_org_settings_week_start
        CHECK (
            week_start_day IN (
                'MONDAY',
                'SATURDAY',
                'SUNDAY'
            )
        )
);
```

---

# Migration Ordering

Tables must be created in dependency order.

```text
1. organizations
2. organization_addresses
3. organization_contacts
4. organization_settings
```

Drop operations, when required in local development migrations, must use reverse order.

```text
1. organization_settings
2. organization_contacts
3. organization_addresses
4. organizations
```

Production Flyway migrations must never modify an already-applied migration file.

Schema changes require a new migration.

Example:

```text
V5__add_organization_branding.sql
```

---

# Hibernate UUID Mapping

If UUIDs use MySQL `BINARY(16)`, mapping should be explicit and consistent.

Example with Hibernate 6:

```java
@JdbcTypeCode(SqlTypes.BINARY)
@Column(
        name = "id",
        columnDefinition = "BINARY(16)",
        nullable = false,
        updatable = false
)
private UUID id;
```

The same configuration must be applied to:

- `id`
- `tenantId`
- `createdBy`
- `updatedBy`
- Foreign-key UUID fields

Before adopting binary UUID storage, verify generated SQL and integration tests against the exact MySQL version used by OdinSync.

Using `CHAR(36)` is acceptable during the initial implementation when readability and operational simplicity are prioritized over index compactness.

---

# Tenant Isolation Enforcement

Tenant isolation must exist at several layers.

```text
Authenticated JWT
        │
        ▼
CurrentActorProvider
        │
        ▼
Application Use Case
        │
        ▼
Tenant-Scoped Repository Method
        │
        ▼
Database Unique Constraints
```

---

## Rule 1: Never Accept Tenant ID from Public Requests

Public profile and settings endpoints must not contain:

- `tenantId` path variables
- `tenantId` request parameters
- `tenantId` request body fields
- Unvalidated tenant headers

The tenant comes from the authenticated actor.

---

## Rule 2: Use Tenant-Scoped Repository Methods

Use:

```java
findByTenantId(actor.tenantId())
```

or:

```java
findByIdAndTenantId(
        organizationId,
        actor.tenantId())
```

Avoid:

```java
findById(organizationId)
```

for tenant-scoped operations.

---

## Rule 3: Do Not Load and Validate Later

Avoid:

```java
OrganizationJpaEntity entity =
        repository.findById(id)
                .orElseThrow();

if (!entity.getTenantId()
        .equals(currentTenantId)) {
    throw new AccessDeniedException();
}
```

This loads data outside tenant scope.

Prefer:

```java
repository.findByIdAndTenantId(
        id,
        currentTenantId)
```

This prevents another tenant's row from being returned at all.

---

## Rule 4: Return Not Found for Cross-Tenant Identifiers

For ID-based tenant APIs, a record belonging to another tenant should usually appear as:

```http
404 Not Found
```

rather than:

```http
403 Forbidden
```

This avoids revealing that the resource exists.

The current self-service Organization API does not expose organization IDs in request paths, which further reduces this risk.

---

## Rule 5: Add Tenant-Aware Integration Tests

Tests must prove that:

- Tenant A can read Tenant A's organization.
- Tenant A cannot read Tenant B's organization.
- Tenant A cannot update Tenant B's organization.
- Supplying another tenant ID in a payload has no effect.
- Repository queries include tenant scope.
- One tenant cannot have multiple organization roots.

---

# Database-Level Tenant Enforcement

The database ensures:

```sql
UNIQUE (tenant_id)
```

This guarantees one organization per tenant.

The database does not automatically know the tenant from the security context.

Therefore, database constraints alone cannot prevent every cross-tenant read.

Tenant isolation remains primarily an application and repository responsibility.

Future hardening options include:

- Database row-level security where supported
- Separate schemas
- Separate tenant databases
- Hibernate filters
- Tenant-aware database connections

Hibernate filters should not replace explicit tenant-scoped repository queries because filters can be accidentally disabled or omitted.

---

# Audit Implementation

The Organization aggregate stores:

- `createdAt`
- `updatedAt`
- `createdBy`
- `updatedBy`

These fields are business-relevant audit metadata.

They should be controlled by the application and domain workflow.

---

## Recommended Audit Model

```java
public record AuditMetadata(
        Instant createdAt,
        Instant updatedAt,
        UserId createdBy,
        UserId updatedBy
) {

    public static AuditMetadata created(
            UserId actor,
            Instant now
    ) {
        return new AuditMetadata(
                now,
                now,
                actor,
                actor);
    }

    public AuditMetadata updated(
            UserId actor,
            Instant now
    ) {
        return new AuditMetadata(
                createdAt,
                now,
                createdBy,
                actor);
    }
}
```

The aggregate updates its audit metadata when business state changes.

---

# Clock Abstraction

Domain and application code should not call:

```java
Instant.now()
```

directly.

Use an abstraction:

```java
public interface TimeProvider {

    Instant now();
}
```

Infrastructure implementation:

```java
@Component
public class SystemTimeProvider
        implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider() {
        this.clock = Clock.systemUTC();
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
```

Tests can provide a fixed clock.

```java
Clock.fixed(
        Instant.parse(
                "2026-07-20T10:00:00Z"),
        ZoneOffset.UTC);
```

This makes audit tests deterministic.

---

# Spring Data Auditing

Spring Data provides annotations such as:

```java
@CreatedDate
@LastModifiedDate
@CreatedBy
@LastModifiedBy
```

These may be useful for technical persistence auditing.

However, for OdinSync's Organization aggregate, domain-controlled audit metadata is preferable because:

- The actor is part of the business operation.
- Domain events may include the updating actor.
- Audit state remains independent of JPA.
- Aggregate tests can verify audit behavior.
- Persistence does not silently alter domain state.

Spring Data auditing may still be used for infrastructure-only records such as:

- Outbox messages
- Login audit entries
- Technical job records

---

# Database Timestamp Rules

All timestamps must be stored in UTC.

Use:

```java
Instant
```

in Java.

Do not store organization-local timestamps in audit columns.

The organization's configured timezone is used only for:

- Display
- Business-day calculations
- Reports
- Scheduling interpretation

Example:

```text
Stored:
2026-07-20T10:00:00Z

Displayed for Asia/Kolkata:
2026-07-20 15:30
```

---

# Optimistic Locking

The root entity owns the aggregate version.

```java
@Version
@Column(
        name = "version",
        nullable = false
)
private long version;
```

Dependent tables do not require separate versions because they are modified only through the root aggregate.

When Hibernate detects a stale version, it may throw:

- `OptimisticLockException`
- `ObjectOptimisticLockingFailureException`
- `OptimisticLockingFailureException`

The infrastructure or exception-handling layer must translate these into a stable application error.

Recommended API response:

```http
409 Conflict
```

Error code:

```text
ORG_007
```

---

# Infrastructure Exception Translation

Infrastructure exceptions must not leak directly to the presentation layer.

Examples of exceptions that should be translated:

| Infrastructure Exception | Application Meaning |
|---|---|
| `DataIntegrityViolationException` | Organization persistence constraint violated |
| `ObjectOptimisticLockingFailureException` | Concurrent update conflict |
| `JpaSystemException` | Persistence operation failed |
| `ConstraintViolationException` | Database constraint rejected data |
| `CannotAcquireLockException` | Database lock unavailable |
| `QueryTimeoutException` | Database query timed out |

Recommended exceptions:

```java
public class OrganizationPersistenceException
        extends RuntimeException {

    public OrganizationPersistenceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
```

```java
public class OrganizationConcurrentUpdateException
        extends RuntimeException {

    public OrganizationConcurrentUpdateException(
            Throwable cause
    ) {
        super(
                "Organization was modified by another transaction.",
                cause);
    }
}
```

---

## Adapter Translation Example

```java
@Override
public Organization save(
        Organization organization
) {
    try {
        OrganizationJpaEntity entity =
                loadOrCreateEntity(organization);

        persistenceMapper.updateEntity(
                organization,
                entity);

        OrganizationJpaEntity saved =
                springDataRepository.saveAndFlush(
                        entity);

        return persistenceMapper.toDomain(saved);

    } catch (
            ObjectOptimisticLockingFailureException exception
    ) {
        throw new OrganizationConcurrentUpdateException(
                exception);

    } catch (
            DataIntegrityViolationException exception
    ) {
        throw new OrganizationPersistenceException(
                "Organization data violated a persistence constraint.",
                exception);
    }
}
```

Using `saveAndFlush` may expose database errors inside the current transaction boundary.

It should not be used automatically for every repository operation because frequent flushing can reduce performance.

Use it when immediate constraint detection is required.

---

# Domain Event Persistence

Publishing an in-memory Spring event after saving the aggregate is sufficient for an initial modular-monolith implementation.

Example:

```java
Organization saved =
        organizationRepository.save(organization);

domainEventPublisher.publishAll(
        saved.pullDomainEvents());
```

However, this creates a reliability concern:

```text
Database commit succeeds
        │
        ▼
Process fails before event publication
        │
        ▼
Event is lost
```

For events that affect durable cross-module workflows, OdinSync should use a transactional outbox.

---

# Transactional Outbox

The aggregate update and outbox record are persisted in the same database transaction.

```text
BEGIN TRANSACTION
        │
        ├── Update Organization
        ├── Insert Outbox Event
        │
        └── COMMIT
```

A separate publisher sends pending outbox records to:

- Kafka
- Google Pub/Sub
- RabbitMQ
- Internal event consumers

Suggested table:

```text
outbox_events
```

Columns:

| Column | Purpose |
|---|---|
| `id` | Event identifier |
| `aggregate_type` | `ORGANIZATION` |
| `aggregate_id` | Organization identifier |
| `tenant_id` | Tenant scope |
| `event_type` | Event name |
| `payload` | Serialized event body |
| `occurred_at` | Domain occurrence time |
| `published_at` | Broker publication time |
| `status` | Pending or published |
| `attempt_count` | Delivery attempts |

The Organization module should depend only on an event publisher port.

The outbox implementation remains infrastructure-specific.

---

# Event Publication Timing

Internal application events should be processed after a successful commit when consumers must not observe uncommitted data.

Spring example:

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT)
public void handle(
        OrganizationSettingsUpdated event
) {
    // Handle committed event
}
```

For durable asynchronous delivery, prefer the outbox.

Do not publish directly to Kafka inside the database transaction and assume both operations are atomic.

---

# Persistence Package Structure

```text
organization/
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   │   ├── OrganizationJpaEntity.java
    │   │   ├── OrganizationAddressJpaEntity.java
    │   │   ├── OrganizationContactJpaEntity.java
    │   │   ├── OrganizationSettingsJpaEntity.java
    │   │   └── OrganizationStatusJpa.java
    │   │
    │   ├── repository/
    │   │   ├── SpringDataOrganizationRepository.java
    │   │   └── OrganizationRepositoryAdapter.java
    │   │
    │   └── mapper/
    │       ├── OrganizationPersistenceMapper.java
    │       └── OrganizationPersistenceMapperImpl.java
    │
    ├── security/
    │   └── SpringSecurityCurrentActorAdapter.java
    │
    ├── event/
    │   ├── SpringDomainEventPublisher.java
    │   └── outbox/
    │       ├── OutboxJpaEntity.java
    │       ├── OutboxRepository.java
    │       └── OutboxEventPublisher.java
    │
    └── time/
        └── SystemTimeProvider.java
```

---

# Security Context Adapter

The Identity module should own authentication principal interpretation.

The Organization module consumes the stable `CurrentActorProvider` contract.

Example infrastructure adapter:

```java
@Component
public class SpringSecurityCurrentActorAdapter
        implements CurrentActorProvider {

    @Override
    public AuthenticatedActor getCurrentActor() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new UnauthenticatedActorException();
        }

        if (!(authentication.getPrincipal()
                instanceof AuthenticatedPrincipal principal)) {
            throw new InvalidAuthenticatedPrincipalException();
        }

        return new AuthenticatedActor(
                principal.userId(),
                principal.tenantId(),
                principal.roles(),
                principal.permissions());
    }
}
```

Prefer using an Identity-owned principal type rather than reading raw JWT claims throughout business modules.

This isolates Organization from:

- JWT claim naming
- Token format
- Spring Security internals
- Future authentication-provider changes

---

# Query Performance

The primary Organization query is:

```sql
SELECT ...
FROM organizations
WHERE tenant_id = ?
```

The unique tenant index makes this lookup efficient.

The aggregate-owned tables are joined by primary key.

Expected query characteristics:

- One tenant lookup
- At most one row per table
- No tenant-level table scans
- No collection N+1 behavior

Performance tests should verify that loading one aggregate does not produce uncontrolled extra queries.

---

# Schema Validation

OdinSync should use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate validates that entity mappings match the Flyway-managed schema.

Hibernate must not create or update the production schema automatically.

Avoid:

```yaml
ddl-auto: update
```

because it:

- Produces uncontrolled schema changes
- Does not provide reviewable migrations
- Can behave differently across environments
- Makes rollback and deployment auditing difficult

Flyway remains the source of truth for database structure.

---

# Persistence Integration Tests

Persistence integration tests should use:

- Testcontainers
- The same database engine used in production
- Flyway migrations
- Real Spring Data repositories

H2 should not be the primary persistence compatibility test because its SQL behavior differs from MySQL.

---

## Required Repository Tests

### Save New Organization

Verify:

- Root row is inserted.
- Address row is inserted.
- Contact row is inserted.
- Settings row is inserted.
- Tenant ID is preserved.
- Audit metadata is preserved.

---

### Load by Tenant

Verify:

- Complete aggregate is reconstructed.
- Address, contact, and settings are available.
- No creation event is generated during reconstruction.

---

### Update Organization

Verify:

- Existing root row is updated.
- Owned rows are updated.
- No duplicate child rows are created.
- Immutable tenant ID is unchanged.

---

### Tenant Isolation

Verify:

```text
Tenant A lookup
        │
        ├── returns Organization A
        └── never returns Organization B
```

---

### Unique Tenant Constraint

Verify that creating a second organization for the same tenant fails.

---

### Optimistic Locking

Test flow:

1. Load the same organization into two persistence contexts.
2. Update and commit the first entity.
3. Update and commit the second entity.
4. Verify the second update fails.
5. Verify the application maps the failure to a concurrency exception.

---

### Cascade Delete

Where organization deletion is supported, verify that deleting the root removes:

- Address
- Contact
- Settings

Deletion should remain disabled until the corresponding business use case is formally designed.

---

# Part 4 Completion Criteria

Part 4 is complete when the implementation defines:

- JPA entities for the full aggregate
- Root-to-child ownership mappings
- Persistence-to-domain mapping
- Aggregate reconstitution
- Spring Data repository queries
- Repository adapter behavior
- Tenant-scoped persistence access
- Database tables and constraints
- Flyway migration scripts
- UUID storage strategy
- Audit metadata handling
- Optimistic locking
- Infrastructure exception translation
- Domain-event publication strategy
- Transactional outbox direction
- Persistence integration tests

The Organization module now has a complete persistence and infrastructure design that preserves domain boundaries, enforces tenant isolation, supports concurrency control, and remains suitable for later service extraction.


# Part 5: Testing, Security, Observability, and Operational Readiness

## Overview

Part 5 defines how the Organization module will be verified and operated in production.

It covers:

- Domain unit tests
- Application service tests
- Persistence integration tests
- REST controller tests
- Security tests
- Tenant-isolation tests
- Concurrency tests
- Event publication tests
- Error contracts
- Logging
- Metrics
- Tracing
- Health indicators
- Acceptance criteria
- Future enhancements
- Final implementation checklist

The objective is to ensure that the Organization module is:

- Correct
- Secure
- Tenant-isolated
- Observable
- Maintainable
- Production-ready

---

# Testing Strategy

The Organization module should follow a testing pyramid.

```text
                    End-to-End Tests
                          ▲
                          │
                 API Integration Tests
                          ▲
                          │
              Persistence and Security Tests
                          ▲
                          │
                Application Service Tests
                          ▲
                          │
                    Domain Unit Tests
```

The largest number of tests should exist at the domain and application layers.

Fewer tests should exist at the full end-to-end level because they are slower and more expensive to maintain.

---

# Test Categories

The Organization module requires the following categories:

| Test Category | Purpose |
|---|---|
| Domain unit tests | Verify business rules and invariants |
| Application tests | Verify use-case orchestration |
| Mapper tests | Verify domain and persistence conversion |
| Repository integration tests | Verify JPA mappings and database behavior |
| Controller tests | Verify HTTP contracts |
| Security tests | Verify permissions and authentication |
| Tenant-isolation tests | Prevent cross-tenant access |
| Concurrency tests | Verify optimistic locking |
| Event tests | Verify event creation and publication |
| Observability tests | Verify logs, metrics, traces, and health behavior |
| End-to-end tests | Verify complete user workflows |

---

# Recommended Test Tooling

The Organization module should use:

- JUnit 5
- AssertJ
- Mockito
- Spring Boot Test
- Spring Security Test
- MockMvc
- Testcontainers
- MySQL Testcontainer
- Flyway
- Awaitility for asynchronous event tests
- ArchUnit for architecture rules
- WireMock only when an external dependency is involved

Recommended Maven dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

---

# Test Package Structure

Tests should mirror the production package structure.

```text
src/test/java
└── com/odinsync/organization
    ├── domain
    │   ├── model
    │   ├── valueobject
    │   └── service
    │
    ├── application
    │   ├── command
    │   ├── query
    │   └── service
    │
    ├── infrastructure
    │   ├── persistence
    │   ├── mapper
    │   ├── repository
    │   └── security
    │
    ├── presentation
    │   ├── controller
    │   └── advice
    │
    ├── integration
    │
    ├── architecture
    │
    └── support
        ├── builder
        ├── fixture
        └── util
```

The package layout should make it easy to locate the tests corresponding to production classes.

---

# Domain Unit Tests

Domain tests verify business behavior without Spring Boot.

These tests should execute in milliseconds.

They must not require:

- Spring Context
- Database
- JPA
- MockMvc
- Security
- Docker

Example:

```java
class OrganizationTest {

    @Test
    void shouldUpdateDisplayName() {
    }

    @Test
    void shouldRejectBlankLegalName() {
    }

    @Test
    void shouldPublishProfileUpdatedEvent() {
    }

    @Test
    void shouldUpdateAuditMetadata() {
    }

}
```

---

# Domain Test Coverage

Recommended coverage includes:

## Organization Creation

Verify:

- Aggregate is created.
- Audit metadata is initialized.
- Version starts correctly.
- OrganizationCreated event is recorded.

---

## Update Profile

Verify:

- Legal name changes.
- Display name changes.
- Address changes.
- Contact changes.
- Audit metadata changes.
- Domain event recorded.

---

## Update Settings

Verify:

- Currency changes.
- Time zone changes.
- Locale changes.
- Date format changes.
- Time format changes.
- Week start changes.

---

## Validation

Verify:

- Blank names rejected.
- Invalid currency rejected.
- Invalid locale rejected.
- Invalid time zone rejected.
- Invalid phone rejected.
- Invalid email rejected.

---

## State Rules

Verify:

- Archived organization cannot be modified.
- Suspended organization follows expected rules.
- Active organization accepts updates.

---

# Value Object Tests

Every value object should have dedicated tests.

Example:

```java
class CurrencyCodeTest {

    @Test
    void shouldAcceptIsoCurrency() {
    }

    @Test
    void shouldRejectUnknownCurrency() {
    }

}
```

Repeat for:

- OrganizationName
- Address
- EmailAddress
- PhoneNumber
- Locale
- TimeZone
- TaxRegistrationNumber

Value objects should be immutable.

---

# Application Service Tests

Application tests verify orchestration.

Dependencies are mocked.

Example:

```java
@ExtendWith(MockitoExtension.class)
class UpdateOrganizationServiceTest {

    @Mock
    OrganizationRepository repository;

    @Mock
    CurrentActorProvider actorProvider;

    @Mock
    OrganizationAuthorizationService authorization;

    @InjectMocks
    UpdateOrganizationService service;

}
```

Application tests should verify:

- Permission checks
- Repository interaction
- Aggregate invocation
- Save operation
- Event publication
- DTO mapping

Business rules should already be covered by domain tests.

---

# Repository Interaction Tests

Example:

```java
@Test
void shouldLoadOrganization() {

    verify(repository)
            .findByTenantId(any());

}
```

Verify:

- Correct repository methods
- No unnecessary queries
- Correct save behavior

---

# Authorization Tests

Verify:

```text
Current Actor

↓

Permission Check

↓

Repository Access

↓

Save
```

Unauthorized users must never reach persistence.

---

# Mapper Tests

Persistence mapping is critical.

Verify:

```text
Domain

↓

JPA

↓

Domain
```

produces an equivalent aggregate.

Round-trip mapping should preserve:

- IDs
- Tenant
- Address
- Contact
- Settings
- Status
- Audit metadata
- Version

---

# Repository Integration Tests

Repository tests require:

- Spring Boot
- Testcontainers
- MySQL
- Flyway

Avoid H2 for persistence compatibility.

Example:

```java
@DataJpaTest
@Testcontainers
class OrganizationRepositoryIT {
}
```

---

# Repository Test Cases

## Save Aggregate

Verify:

- Organization inserted.
- Address inserted.
- Contact inserted.
- Settings inserted.

---

## Find by Tenant

Verify:

- Aggregate reconstructed.
- Child objects loaded.

---

## Exists by Tenant

Verify:

- True when organization exists.
- False otherwise.

---

## Tenant Isolation

Verify:

Tenant A

↓

Cannot load Tenant B.

---

## Optimistic Locking

Two transactions.

First succeeds.

Second throws:

```text
OptimisticLockException
```

---

## Cascade Behavior

Deleting organization removes:

- Address
- Contact
- Settings

---

# Controller Tests

Use:

```text
MockMvc
```

Verify:

- Status codes
- JSON response
- Validation
- Authentication
- Authorization
- Error responses

---

# Controller Test Cases

GET:

```text
/api/v1/organizations/me
```

Verify:

- 200
- Correct payload
- Correct tenant

---

PUT:

```text
/api/v1/organizations/me
```

Verify:

- Success
- Validation
- Errors
- Updated response

---

GET Settings

Verify:

```text
200 OK
```

---

PUT Settings

Verify:

```text
200 OK
```

and updated settings.

---

# Validation Tests

Invalid payloads:

Blank name

↓

400

Invalid email

↓

400

Invalid currency

↓

400

Missing required field

↓

400

---

# Security Tests

Spring Security Test support:

```java
@WithMockUser
```

or preferred custom authentication matching OdinSync.

Verify:

Unauthenticated

↓

401

No permission

↓

403

Correct permission

↓

200

---

# Permission Matrix

| Endpoint | Permission |
|---|---|
| GET organization | organization:read |
| PUT organization | organization:update |
| GET settings | organization:settings:read |
| PUT settings | organization:settings:update |

Every endpoint should have explicit tests.

---

# Tenant Isolation Tests

Critical scenarios:

Tenant A

↓

GET own organization

↓

200

---

Tenant A

↓

GET Tenant B

↓

404

---

Tenant A

↓

Update Tenant B

↓

404

---

Repository queries must always include:

```text
tenant_id
```

---

# Concurrency Tests

Verify optimistic locking.

Scenario:

Thread A

↓

Load version 4

Thread B

↓

Load version 4

Thread A

↓

Save

↓

Version 5

Thread B

↓

Save

↓

Conflict

Expected:

```text
409
```

---

# Domain Event Tests

Verify:

Profile update

↓

OrganizationProfileUpdated

Settings update

↓

OrganizationSettingsUpdated

No change

↓

No event

---

# Event Publication Tests

Application test:

Aggregate saved

↓

Publisher invoked

↓

Correct event

Verify ordering.

---

# Outbox Tests (Future)

Verify:

Save aggregate

↓

Insert outbox row

↓

Commit

↓

Publisher sends

↓

Row marked published

---

# Architecture Tests

Use:

```text
ArchUnit
```

Rules:

Domain

↓

Must not depend on Spring

Application

↓

Must not depend on JPA

Presentation

↓

Must not access repositories directly

Infrastructure

↓

Must not be referenced by domain

Example:

```java
classes()
    .that()
    .resideInAPackage("..domain..")
    .should()
    .onlyDependOnClassesThat()
    .resideOutsideOfPackage("org.springframework..");
```

---

# Test Fixtures

Avoid duplicated setup.

Create builders.

Example:

```java
OrganizationBuilder
```

Methods:

```java
withLegalName()

withDisplayName()

withAddress()

withSettings()

build()
```

---

# Test Data Principles

Each test should arrange only data relevant to that scenario.

Avoid:

- Shared mutable state
- Huge fixtures
- Hidden defaults

Prefer explicit builders.

---

# Code Coverage

Recommended minimums:

| Layer | Target |
|---|---:|
| Domain | 95%+ |
| Application | 90%+ |
| Infrastructure | 80%+ |
| Controller | 80%+ |

Coverage is a health indicator, not a quality guarantee.

Behavior-focused assertions are more valuable than simply increasing percentages.

---

# Continuous Integration

CI pipeline should execute:

1. Unit tests
2. Architecture tests
3. Integration tests
4. Security tests
5. Static analysis
6. Code coverage
7. Build artifact

A merge should fail if any required stage fails.

---

# Acceptance Criteria

The Organization module is complete when:

- Organization can be created.
- Profile can be viewed.
- Profile can be updated.
- Settings can be viewed.
- Settings can be updated.
- Validation rules are enforced.
- Permissions are enforced.
- Tenant isolation is enforced.
- Optimistic locking works.
- Domain events are published.
- Flyway migration succeeds.
- Repository tests pass.
- Controller tests pass.
- Integration tests pass.
- Architecture rules pass.
- CI pipeline succeeds.

---

# Future Enhancements

Planned enhancements include:

- Organization branding
- Logo management
- Multi-office support
- Branch hierarchy
- Fiscal year configuration
- Holiday calendar
- Business hours
- Localization preferences
- Organization hierarchy
- Organization deletion workflow
- Soft delete
- Audit history
- Transactional outbox
- Kafka integration
- Redis caching
- Read-model projections
- Event sourcing investigation

---

# Operational Readiness

Production readiness requires:

- Structured logging
- Distributed tracing
- Metrics
- Health indicators
- Alerting
- Backup strategy
- Database monitoring
- Slow query monitoring
- Security auditing

---

# Final Module Checklist

Before considering the Organization module production-ready, verify:

- Domain model follows DDD principles.
- Clean Architecture boundaries are maintained.
- Persistence is isolated from the domain.
- Tenant isolation is enforced at every layer.
- Security is permission-based.
- Validation exists in the appropriate layers.
- Audit metadata is recorded.
- Optimistic locking prevents lost updates.
- Domain events are published correctly.
- Repository mappings are tested.
- APIs are documented.
- Flyway migrations are version-controlled.
- Logging, metrics, and tracing are configured.
- CI pipeline passes all quality gates.
- Documentation is complete and synchronized with implementation.

---

# Organization Module Complete

With Parts 1 through 5 complete, the Organization module now has a comprehensive specification covering:

- Business requirements
- Domain model
- Application layer
- Persistence and infrastructure
- Testing strategy
- Security
- Observability
- Operational readiness

This specification is intended to serve as the implementation blueprint for the Organization bounded context within OdinSync.
