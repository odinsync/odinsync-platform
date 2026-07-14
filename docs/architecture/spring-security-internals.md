# Spring Security Internals

## Purpose

This document explains how authentication and authorization requests move through Spring Security in OdinSync.

## Authentication vs Authorization

Authentication answers:

Who is the user?

Authorization answers:

What is the user allowed to do?

## Login Flow

POST /api/v1/auth/login
→ LoginController
→ LoginUseCase
→ UserRepositoryPort
→ PasswordEncoderPort
→ TokenGeneratorPort
→ Access and refresh tokens returned

The login endpoint does not use the JWT filter because the user does not have a token yet.

## Protected Request Flow

GET /api/v1/customers
→ Tomcat
→ Spring Security Filter Chain
→ CORS processing
→ JWT authentication filter
→ Authorization filter
→ Controller

## SecurityFilterChain

SecurityFilterChain defines:

- Public endpoints
- Protected endpoints
- Stateless session policy
- CORS configuration
- CSRF policy
- Custom JWT filter position

## JWT Authentication Filter

Responsibilities:

1. Read the Authorization header.
2. Check for the Bearer prefix.
3. Extract the JWT.
4. Validate signature and expiry.
5. Read userId, tenantId, and roles.
6. Create an Authentication object.
7. Store it in SecurityContextHolder.
8. Continue the request chain.

The filter must not contain business logic.

## SecurityContext

SecurityContextHolder stores the authenticated identity for the current request.

Controllers and services can access:

- userId
- tenantId
- roles
- permissions

The context is cleared after request completion.

## AuthenticationManager

AuthenticationManager coordinates credential authentication.

For username/password authentication, it delegates to an AuthenticationProvider.

## AuthenticationProvider

An AuthenticationProvider validates a particular authentication mechanism.

For OdinSync login:

- Load the user.
- Compare the submitted password with the BCrypt hash.
- Reject disabled users.
- Return authenticated principal details.

## UserDetailsService

UserDetailsService loads authentication data by email.

It should return:

- user ID
- tenant ID
- email
- password hash
- user status
- tenant status
- roles

It must not expose raw passwords.

## PasswordEncoder

BCrypt is used to:

- Hash passwords during registration.
- Compare raw login passwords with stored hashes.

Passwords are never decrypted.

## AuthorizationFilter

After authentication succeeds, Spring checks whether the authenticated user may access the requested endpoint.

Examples:

- OWNER can manage organization settings.
- SALES_MANAGER can create sales orders.
- ACCOUNTANT can record payments.

## Stateless Authentication

OdinSync does not maintain an HTTP session.

Each protected request must contain:

Authorization: Bearer <access-token>

This supports horizontal scaling and Kubernetes deployment.

## Error Cases

- Missing token → 401 Unauthorized
- Invalid token → 401 Unauthorized
- Expired token → 401 Unauthorized
- Valid token without required role → 403 Forbidden
- Disabled user → 401 Unauthorized
- Suspended tenant → 403 Forbidden

## Planned Components

- LoginController
- LoginUseCase
- JwtAuthenticationFilter
- JwtTokenService
- OdinSyncUserDetailsService
- CustomAuthenticationEntryPoint
- CustomAccessDeniedHandler
- RefreshTokenUseCase