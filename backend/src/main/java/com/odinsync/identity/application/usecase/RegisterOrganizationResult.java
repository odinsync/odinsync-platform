package com.odinsync.identity.application.usecase;

import java.util.UUID;

public record RegisterOrganizationResult(
		UUID tenantId,
		UUID organizationId,
		UUID userId,
		String message) {
}
