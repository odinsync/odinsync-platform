package com.odinsync.identity.application.usecase;

import java.time.Instant;
import java.util.UUID;

public record RotatedRefreshToken(
		UUID userId,
		UUID tenantId,
		String rawRefreshToken,
		Instant expiresAt) {
}
