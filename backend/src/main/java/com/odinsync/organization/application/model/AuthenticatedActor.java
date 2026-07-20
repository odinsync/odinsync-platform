package com.odinsync.organization.application.model;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedActor(
		UUID userId,
		UUID tenantId,
		Set<String> roles,
		Set<String> permissions) {

	public AuthenticatedActor {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
		permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions must not be null"));
	}
}
