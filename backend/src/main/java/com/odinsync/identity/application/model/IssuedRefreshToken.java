package com.odinsync.identity.application.model;

import java.time.Instant;
import java.util.UUID;

import com.odinsync.identity.domain.model.RefreshToken;

public record IssuedRefreshToken(
		UUID id,
		UUID familyId,
		String rawToken,
		Instant expiresAt,
		RefreshToken refreshToken) {

	/**
	 * Validates that issuance results never contain a missing raw token or domain model.
	 */
	public IssuedRefreshToken {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken must not be blank");
		}
		if (refreshToken == null) {
			throw new IllegalArgumentException("refreshToken must not be null");
		}
	}
}
