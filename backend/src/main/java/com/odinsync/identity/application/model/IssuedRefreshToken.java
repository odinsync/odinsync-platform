package com.odinsync.identity.application.model;

import java.time.Instant;
import java.util.UUID;

public record IssuedRefreshToken(
		UUID id,
		UUID familyId,
		String rawToken,
		Instant expiresAt) {
}
