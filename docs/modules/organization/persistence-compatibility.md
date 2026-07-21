# Organization Persistence Compatibility

## Migration

- Version: `V4`
- File: `backend/src/main/resources/db/migration/V4__align_organization_aggregate_schema.sql`
- Table: `organizations`
- Strategy: extend the existing Identity-created table instead of creating a second Organization table.

## Current Ownership Status

The `organizations` table is still used by Identity registration. The Organization module now has its own aggregate persistence model that maps to the same table. ORG-07 does not transfer registration ownership and does not add dual writes.

## Entity-to-Column Matrix

| Entity field | Database column | SQL type | Nullable | Constraint |
| --- | --- | --- | --- | --- |
| `id` | `id` | `UUID` | no | primary key from V1 |
| `tenantId` | `tenant_id` | `UUID` | no | foreign key and unique index from V1 |
| `legalName` | `legal_name` | `VARCHAR(200)` | no | not blank in domain |
| `displayName` | `display_name` | `VARCHAR(120)` | no | not blank in domain |
| `taxRegistrationNumber` | `tax_registration_number` | `VARCHAR(50)` | yes | optional value object |
| `address.addressLine1` | `address_line1` | `VARCHAR(200)` | no | not blank in domain |
| `address.addressLine2` | `address_line2` | `VARCHAR(200)` | yes | optional in domain |
| `address.city` | `address_city` | `VARCHAR(100)` | no | not blank in domain |
| `address.stateOrRegion` | `address_state_or_region` | `VARCHAR(100)` | no | not blank in domain |
| `address.postalCode` | `address_postal_code` | `VARCHAR(20)` | no | not blank in domain |
| `address.countryCode` | `address_country_code` | `VARCHAR(2)` | no | `chk_organizations_country_code` |
| `contact.email` | `contact_email` | `VARCHAR(254)` | no | `chk_organizations_contact_email` |
| `contact.phone` | `contact_phone` | `VARCHAR(30)` | no | not blank in domain |
| `contact.website` | `contact_website` | `VARCHAR(500)` | yes | optional value object |
| `settings.currencyCode` | `currency_code` | `VARCHAR(3)` | no | `chk_organizations_currency_code` |
| `settings.timeZone` | `time_zone` | `VARCHAR(100)` | no | IANA zone identifier in domain |
| `settings.locale` | `locale` | `VARCHAR(20)` | no | BCP 47 language tag in domain |
| `settings.dateFormat` | `date_format` | `VARCHAR(30)` | no | `chk_organizations_date_format` |
| `settings.timeFormat` | `time_format` | `VARCHAR(20)` | no | `chk_organizations_time_format` |
| `settings.weekStart` | `week_start` | `VARCHAR(15)` | no | `chk_organizations_week_start` |
| `status` | `status` | `VARCHAR(30)` | no | `chk_organizations_status` |
| `audit.createdAt` | `created_at` | `TIMESTAMPTZ` | no | existing column converted in V4 |
| `audit.createdBy` | `created_by` | `UUID` | no | audit actor ID, no FK |
| `audit.updatedAt` | `updated_at` | `TIMESTAMPTZ` | no | `chk_organizations_audit_chronology` |
| `audit.updatedBy` | `updated_by` | `UUID` | no | audit actor ID, no FK |
| `version` | `version` | `BIGINT` | no | `chk_organizations_version` |

## Constraints and Indexes

- V1 already provides the primary key on `id`.
- V1 already provides `fk_organizations_tenant` from `tenant_id` to `tenants(id)`.
- V1 already provides `uk_organizations_tenant_id`, confirming one Organization per tenant in the current schema.
- V4 adds check constraints for status, enum-backed settings, country code, currency code, contact email shape, audit chronology, and optimistic-lock version.
- No status index is added because no implemented repository query filters by status.

## Identity Compatibility

Identity currently maps these columns: `id`, `tenant_id`, `name`, `legal_name`, `email`, `created_at`, and `updated_at`.

The Organization module maps the richer aggregate columns introduced in V4. Legacy columns `name`, `email`, `gst_number`, and `address` are retained. V4 backfills deterministic values from those columns where possible.

Known compatibility risks:

- Identity registration does not supply Organization aggregate fields such as address, settings, lifecycle status, or audit actor IDs.
- V4 uses compatibility defaults for required aggregate columns so existing registration writes do not fail before ORG-08.
- These defaults are migration compatibility values, not final domain ownership semantics.
- ORG-08 should move registration-time Organization creation behind the Organization application port and remove the need for compatibility defaults.

## Bean Registration

Both Identity and Organization contain a class named `OrganizationPersistenceMapper`. To avoid default Spring bean-name collision, their component names are explicit:

- `identityOrganizationPersistenceMapper`
- `organizationAggregatePersistenceMapper`

## Deferred Work

- Transfer registration-time Organization creation from Identity persistence to the Organization application port.
- Remove duplicate Identity Organization persistence after ownership transfer.
- Add PostgreSQL-backed Flyway/JPA validation with Testcontainers when the persistence testing phase is approved.
- Replace compatibility defaults with application-supplied aggregate values once ORG-08 is complete.
