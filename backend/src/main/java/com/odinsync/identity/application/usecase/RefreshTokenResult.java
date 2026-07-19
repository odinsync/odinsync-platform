package com.odinsync.identity.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefreshTokenResult(
		String accessToken,
		String tokenType,
		long expiresIn,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		UUID tenantId,
		UUID userId,
		List<String> roles) {
}
