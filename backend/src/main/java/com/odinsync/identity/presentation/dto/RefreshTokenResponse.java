package com.odinsync.identity.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefreshTokenResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		UUID tenantId,
		UUID userId,
		List<String> roles) {
}
