package com.odinsync.identity.presentation.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		UUID tenantId,
		UUID userId,
		List<String> roles) {
}
