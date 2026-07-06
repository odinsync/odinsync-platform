package com.odinsync.identity.domain.model;

import java.util.UUID;

public record Role(
		UUID id,
		UUID tenantId,
		RoleName name,
		String description) {
	public static Role ownerRole(UUID tenantId) {
		return new Role(
				UUID.randomUUID(),
				tenantId,
				RoleName.OWNER,
				"Tenant owner with full access."
		);
	}
}

//	private static final String OWNER_ROLE_DESCRIPTION = "Tenant owner with full access.";
