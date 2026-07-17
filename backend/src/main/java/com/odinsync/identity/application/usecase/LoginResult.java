package com.odinsync.identity.application.usecase;

import java.util.List;
import java.util.UUID;

public record LoginResult(
		String accessToken,
		String tokenType,
		long expiresIn,
		UUID tenantId,
		UUID userId,
		List<String> roles) {
}
