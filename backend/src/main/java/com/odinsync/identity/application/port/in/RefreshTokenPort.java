package com.odinsync.identity.application.port.in;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.command.LogoutCommand;
import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.model.ActiveSession;
import com.odinsync.identity.application.model.RefreshTokenResult;

public interface RefreshTokenPort {

	/**
	 * Rotates a submitted refresh token and returns a new access/refresh token pair.
	 */
	RefreshTokenResult refresh(RefreshTokenCommand command);

	/**
	 * Logs out the session represented by the submitted refresh token.
	 */
	void logout(LogoutCommand command);

	/**
	 * Revokes all active refresh-token sessions for a user within one tenant.
	 */
	void logoutAll(UUID userId, UUID tenantId);

	/**
	 * Lists active sessions for a user within one tenant.
	 */
	List<ActiveSession> activeSessions(UUID userId, UUID tenantId);

	/**
	 * Revokes one selected session scoped to a user and tenant.
	 */
	void revokeSession(UUID sessionId, UUID userId, UUID tenantId);
}
