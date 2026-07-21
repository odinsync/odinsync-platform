package com.odinsync.organization.application.result;

import java.util.Objects;
import java.util.UUID;

public record ProvisionedOrganizationResult(
		UUID organizationId,
		UUID tenantId) {

	public ProvisionedOrganizationResult {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
	}
}
