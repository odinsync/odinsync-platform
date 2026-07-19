/**
 * Infrastructure layer for the Identity bounded context.
 *
 * <p>Provides adapters for persistence, password hashing, JWT issuance,
 * refresh-token hashing and generation, and Spring Security integration.
 * Infrastructure classes implement application ports and must not leak JPA
 * entities or framework-specific details into the domain layer.
 */
package com.odinsync.identity.infrastructure;
