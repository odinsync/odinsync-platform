# Organization Task Tracker

## Active Sequence

| Task | Status | Notes |
| --- | --- | --- |
| ORG-00 Repository Discovery and Implementation Baseline | Complete | Baseline recorded in `docs/modules/organization/implementation-baseline.md`. |
| ORG-01 Domain Primitives and Value Objects | Complete | Pure Organization domain value objects and enums are implemented without Spring, JPA, API, security, migrations, or persistence. |
| ORG-02 Organization Aggregate and Domain Events | Complete | Organization aggregate and local domain event recording are implemented without publisher/outbox infrastructure. |
| ORG-03 Application Ports and Contracts | Complete | Application contracts for current actor, authorization, repository, time, and event publication are defined without adapters. |
| ORG-04 Profile Application Use Cases | Complete | Get/update Organization profile use cases are implemented using ORG-03 contracts. |
| ORG-05 Settings Application Use Cases | Next | Implement get/update Organization settings use cases using ORG-03 contracts. |

## Deferred Beyond ORG-03

| Item | Decision | Reason |
| --- | --- | --- |
| CurrentActorProvider adapter | Wait until ORG-12 | ORG-03 defines the application port only; Spring Security JWT integration belongs to the security integration phase. |
| Organization table ownership decision | Wait until ORG-06/ORG-07 | Ownership affects Flyway migrations, JPA mapping, existing Identity registration persistence, and any backfill/migration strategy. |
| Permission-based authorization | Wait until security/API phases | Current access tokens contain roles only; permission authorities require a dedicated security/API phase before Organization endpoints can depend on them. |
| Testcontainers | Wait until persistence testing phase | ORG-01 does not need database integration tests; PostgreSQL-backed testing should be introduced when persistence adapters and migrations are implemented. |

## ORG-01 Guardrails

- Do not implement `CurrentActorProvider`.
- Do not parse JWT claims.
- Do not introduce Spring Security adapters.
- Do not add controllers, use cases, repositories, JPA entities, or migrations.
- Keep new code inside the Organization domain package unless ORG-01 explicitly requires otherwise.
