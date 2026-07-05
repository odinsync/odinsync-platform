# Flyway

Flyway will manage database migrations.

## Migration Expectations

- Migrations must be immutable after merge.
- Each migration should have a narrow purpose.
- Destructive changes require a documented rollout plan.
- Local, CI, and production environments should use the same migration mechanism.
