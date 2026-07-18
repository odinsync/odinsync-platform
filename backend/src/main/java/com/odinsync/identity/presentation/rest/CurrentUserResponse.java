package com.odinsync.identity.presentation.rest;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
		UUID userId,
		UUID tenantId,
		String email,
		List<String> roles) {
}
