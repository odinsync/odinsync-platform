/**
 * Presentation layer for the Identity bounded context.
 *
 * <p>Exposes HTTP controllers and request/response DTOs for authentication,
 * registration, refresh-token rotation, logout, and session-management APIs.
 * Controllers should translate transport concerns into application commands
 * and avoid business workflow logic.
 */
package com.odinsync.identity.presentation;
