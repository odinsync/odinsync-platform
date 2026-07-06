package com.odinsync.identity.presentation.dto;

import java.util.UUID;

public record RegisterOrganizationResponse(
		UUID tenantId,
		UUID organizationId,
		UUID userId,
		String message) {
}
