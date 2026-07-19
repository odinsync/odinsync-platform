package com.odinsync.identity.application.model;

import java.util.UUID;

public record RegisterOrganizationResult(
		UUID tenantId,
		UUID organizationId,
		UUID userId,
		String message) {
}
