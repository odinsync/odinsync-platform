package com.odinsync.identity.application.usecase;

import java.time.Instant;
import java.util.UUID;

public record IssuedRefreshToken(
		UUID id,
		UUID familyId,
		String rawToken,
		Instant expiresAt) {
}
