# Modular Monolith

The platform begins as a modular monolith to support fast development, simpler deployment, and easier refactoring while business boundaries are still forming.

## Expectations

- Modules communicate through explicit interfaces.
- Domain logic stays inside the owning module.
- Shared code remains minimal and stable.
- Database ownership should be clear even when physically colocated.
