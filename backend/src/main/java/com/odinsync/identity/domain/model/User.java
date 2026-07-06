package com.odinsync.identity.domain.model;

import java.util.UUID;

public record User(
		UUID id,
		UUID tenantId,
		String fullName,
		String email,
		String passwordHash,
		UserStatus status) {
	public static User createOwner(UUID tenantId, String fullName, String email, String passwordHash) {
		return new User(
				UUID.randomUUID(),
				tenantId,
				fullName,
				email,
				passwordHash,
				UserStatus.ACTIVE

		);
	}
}
