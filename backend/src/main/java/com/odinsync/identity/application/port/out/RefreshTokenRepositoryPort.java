package com.odinsync.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {

	/**
	 * Saves a refresh-token domain model and returns the persisted state.
	 */
	RefreshToken save(RefreshToken refreshToken);

	/**
	 * Finds a refresh token by deterministic token hash without locking.
	 */
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Finds a refresh token by hash with a database write lock for rotation.
	 */
	Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

	/**
	 * Finds active sessions for a user and tenant at the supplied instant.
	 */
	List<RefreshToken> findActiveSessions(UUID userId, UUID tenantId, Instant now);

	/**
	 * Revokes every active refresh token in a token family.
	 */
	void revokeFamily(UUID familyId, Instant revokedAt);

	/**
	 * Revokes every active refresh-token session for one user and tenant.
	 */
	void revokeAllForUserAndTenant(UUID userId, UUID tenantId, Instant revokedAt);

	/**
	 * Deletes expired and revoked records older than the retention cutoff.
	 */
	int deleteExpiredAndRevokedBefore(Instant cutoff);
}
