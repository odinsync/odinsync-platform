# ADR-001: Start with Modular Monolith

## Status

Accepted

## Context

OdinSync is starting as a new open-source SaaS platform. The product domain is still evolving.

Starting directly with microservices would introduce unnecessary complexity.

## Decision

OdinSync will start as a modular monolith.

## Reasons

- Faster development
- Easier debugging
- Easier testing
- Simpler deployment
- Lower operational cost
- Clearer domain modeling

## Consequences

Positive:
- Faster MVP delivery
- Easier onboarding
- Lower infrastructure complexity

Negative:
- Modules must be disciplined
- Poor boundaries could make future extraction harder

## Future Direction

When specific modules require independent scaling or deployment, they may be extracted into microservices.