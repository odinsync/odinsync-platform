package com.odinsync.identity.application.usecase;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.model.ActiveSession;
import com.odinsync.identity.application.model.IssuedRefreshToken;
import com.odinsync.identity.application.model.RotatedRefreshToken;
import com.odinsync.identity.application.model.SessionMetadata;

public interface RefreshTokenService {

	/**
	 * Creates the first refresh-token session for a login using a new token family.
	 */
	IssuedRefreshToken issue(UUID userId, UUID tenantId);

	/**
	 * Issues a refresh token in the supplied family without request metadata.
	 */
	IssuedRefreshToken issue(UUID userId, UUID tenantId, UUID familyId);

	/**
	 * Generates an opaque refresh token, stores only its hash, and returns the raw token once.
	 */
	IssuedRefreshToken issue(UUID userId, UUID tenantId, UUID familyId, SessionMetadata metadata);

	/**
	 * Rotates a refresh token without session metadata, preserving the token family.
	 */
	RotatedRefreshToken rotate(String rawRefreshToken);

	/**
	 * Atomically replaces one active refresh token with a new one and revokes the old token.
	 */
	RotatedRefreshToken rotate(String rawRefreshToken, SessionMetadata metadata);

	/**
	 * Revokes the token family for the submitted refresh token and remains idempotent.
	 */
	void logout(String rawRefreshToken);

	/**
	 * Revokes all active refresh-token sessions for a user within one tenant.
	 */
	void logoutAll(UUID userId, UUID tenantId);

	/**
	 * Returns safe active-session metadata without exposing refresh-token secrets or hashes.
	 */
	List<ActiveSession> getActiveSessions(UUID userId, UUID tenantId);

	/**
	 * Revokes one active session only when it belongs to the authenticated user and tenant.
	 */
	void revokeSession(UUID sessionId, UUID authenticatedUserId, UUID tenantId);

	/**
	 * Deletes expired, revoked refresh-token records older than the configured retention period.
	 */
	int cleanupExpiredTokens();
}
