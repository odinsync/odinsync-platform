# Organization Task Tracker

## Active Sequence

| Task | Status | Notes |
| --- | --- | --- |
| ORG-00 Repository Discovery and Implementation Baseline | Complete | Baseline recorded in `docs/modules/organization/implementation-baseline.md`. |
| ORG-01 Domain Primitives and Value Objects | Next | Implement pure Organization domain value objects only. No Spring, JPA, API, security, migrations, or persistence. |
| ORG-02 Organization Aggregate and Domain Events | Pending | Domain event recording may be designed here, but no publisher/outbox infrastructure yet. |
| ORG-03 Application Ports and Contracts | Pending | Implement application ports including current actor, authorization, repository, time, and event contracts as needed. |

## Deferred Until ORG-03

| Item | Decision | Reason |
| --- | --- | --- |
| CurrentActorProvider | Wait until ORG-03 | ORG-01 must remain focused on framework-free domain primitives. Current actor resolution depends on application/security boundaries, not domain value objects. |
| Organization table ownership decision | Wait until ORG-06/ORG-07 | Ownership affects Flyway migrations, JPA mapping, existing Identity registration persistence, and any backfill/migration strategy. |
| Permission-based authorization | Wait until security/API phases | Current access tokens contain roles only; permission authorities require a dedicated security/API phase before Organization endpoints can depend on them. |
| Testcontainers | Wait until persistence testing phase | ORG-01 does not need database integration tests; PostgreSQL-backed testing should be introduced when persistence adapters and migrations are implemented. |

## ORG-01 Guardrails

- Do not implement `CurrentActorProvider`.
- Do not parse JWT claims.
- Do not introduce Spring Security adapters.
- Do not add controllers, use cases, repositories, JPA entities, or migrations.
- Keep new code inside the Organization domain package unless ORG-01 explicitly requires otherwise.
