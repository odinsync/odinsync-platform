# Schema

Database schema documentation will describe tables, columns, ownership, constraints, indexes, and tenant scoping.

## Conventions

- Every tenant-owned table must include a tenant or organization reference.
- Foreign keys should be explicit where practical.
- Indexes should support real query patterns.
- Schema changes must be applied through migrations.
