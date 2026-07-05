# ADR-002: Multi-Tenant SaaS Architecture

## Status

Accepted

## Context

OdinSync will serve multiple companies from one platform. Each company must have isolated data.

## Decision

Use shared database, shared schema, and tenant-based isolation.

Every business table will contain tenant_id.

## Rules

- Every tenant represents one company/account.
- Every business entity belongs to one tenant.
- tenant_id is extracted from authenticated user context.
- The frontend must never send tenant_id as trusted input.
- Queries must be tenant-aware.

## Reasons

- Lower cost
- Easier maintenance
- Easier migrations
- Good fit for SaaS MVP
- Can support dedicated databases later for enterprise customers

## Consequences

Positive:
- Simple SaaS architecture
- Efficient resource usage
- Easier operations

Negative:
- Tenant filtering must be implemented carefully
- Bugs in isolation could create security risk

## Future Direction

Enterprise customers may later be moved to dedicated databases if needed.