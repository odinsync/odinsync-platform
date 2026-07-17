package com.odinsync.identity.application.usecase;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;

public record AuthenticatedUser(
		UUID userId,
		UUID tenantId,
		String email,
		List<String> roles,
		UserStatus userStatus,
		TenantStatus tenantStatus) {
}
