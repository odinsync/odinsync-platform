package com.odinsync.identity.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
		UUID id,
		UUID userId,
		UUID tenantId,
		String tokenHash,
		UUID familyId,
		Instant issuedAt,
		Instant expiresAt,
		Instant revokedAt,
		UUID replacedByTokenId,
		Instant createdAt,
		Instant updatedAt) {

	public boolean isExpired(Clock clock) {
		return !expiresAt.isAfter(clock.instant());
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isActive(Clock clock) {
		return !isRevoked() && !isExpired(clock);
	}

	public RefreshToken revoke(Instant revokedAt, UUID replacementTokenId) {
		return new RefreshToken(
				id,
				userId,
				tenantId,
				tokenHash,
				familyId,
				issuedAt,
				expiresAt,
				revokedAt,
				replacementTokenId,
				createdAt,
				revokedAt);
	}
}
