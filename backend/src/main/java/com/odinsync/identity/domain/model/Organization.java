package com.odinsync.identity.domain.model;

import java.util.UUID;

public record Organization(
		UUID id,
		UUID tenantId,
		String name,
		String legalName,
		String email
) {
	public static Organization create(UUID tenantId, String name, String legalName, String email) {
		return new Organization(
				UUID.randomUUID(),
				tenantId,
				name,
				legalName,
				email
		);
	}
}