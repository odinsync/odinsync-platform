# Security Baseline

Overview

OdinSync adopts a secure-by-default approach.

Before implementing authentication and authorization, the application establishes a baseline Spring Security configuration that protects every endpoint unless it is explicitly marked as public.

This provides a consistent security foundation for all future modules.

⸻

Objectives

* Secure every API by default.
* Build a stateless REST API.
* Prepare the application for JWT authentication.
* Define CORS behavior explicitly.
* Avoid accidental exposure of endpoints.
* Support future deployment on Kubernetes and cloud platforms.

⸻

Security Architecture

HTTP Request
│
▼
Spring Security Filter Chain
│
▼
CORS Validation
│
▼
CSRF (Disabled)
│
▼
Session Management (STATELESS)
│
▼
Authorization Rules
│
▼
Application

⸻

Current Security Rules

Public Endpoints

The following endpoints are publicly accessible:

* /actuator/health
* /actuator/info

These endpoints are intended for health monitoring and operational readiness checks.

Typical consumers include:

* Kubernetes
* Docker
* Monitoring systems
* Load balancers

⸻

Protected Endpoints

Every other endpoint requires authentication.

Current rule:

Any Request
↓
Authenticated

Authentication itself will be implemented in the Identity module using JWT.

⸻

CSRF

Cross-Site Request Forgery (CSRF) protection is disabled.

Reason:

OdinSync is designed as a stateless REST API.

The application will authenticate requests using JWT tokens transmitted through the Authorization header rather than browser sessions and cookies.

Because session cookies are not used, CSRF protection is unnecessary.

⸻

CORS

Cross-Origin Resource Sharing (CORS) is explicitly configured.

Allowed origins currently include local development environments:

* http://localhost:3000
* http://localhost:4200
* http://localhost:5173

Allowed HTTP methods:

* GET
* POST
* PUT
* PATCH
* DELETE
* OPTIONS

Allowed headers:

* Authorization
* Content-Type
* Accept
* Origin
* X-Requested-With

Credentials are disabled because OdinSync uses bearer tokens rather than cookies.

⸻

Session Management

Session creation policy:

STATELESS

The server does not maintain HTTP sessions.

Every request must authenticate independently.

Benefits:

* Horizontal scalability
* Cloud-native deployment
* Kubernetes friendly
* No sticky sessions
* Better REST compliance

⸻

Authentication Roadmap

Current implementation:

* Spring Security baseline
* Authorization rules
* Stateless configuration
* CORS configuration

Future implementation:

* JWT Authentication Filter
* User authentication
* Refresh tokens
* Role-Based Access Control (RBAC)
* Method-level authorization

⸻

Package Structure

Security components are located in:

com.odinsync.shared.security

Reason:

Security is a cross-cutting concern shared by every bounded context rather than belonging to a single business domain.

⸻

Design Decisions

1. Secure by default.
2. Stateless architecture.
3. Explicit CORS configuration.
4. JWT-based authentication (planned).
5. Public endpoints limited to operational monitoring.

⸻

Future Enhancements

The following features will be implemented in subsequent phases:

* JWT authentication
* Refresh token support
* Password encryption using BCrypt
* AuthenticationEntryPoint
* JwtAuthenticationFilter
* AuthorizationFilter
* Role-Based Access Control (RBAC)
* Method-level security
* Tenant-aware authorization
* Audit logging
* Security event monitoring

⸻

Related Documents

* docs/architecture/overview.md
* docs/architecture/domain-model.md
* docs/architecture/event-storming.md
* docs/api/overview.md
* ENGINEERING_PRINCIPLES.md