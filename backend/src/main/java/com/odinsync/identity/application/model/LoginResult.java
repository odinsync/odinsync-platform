package com.odinsync.identity.application.model;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public record LoginResult(
		String accessToken,
		String tokenType,
		long expiresIn,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		UUID tenantId,
		UUID userId,
		List<String> roles) {
}
