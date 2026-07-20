package com.odinsync.organization.application.result;

import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.model.OrganizationStatus;

public record OrganizationSummaryResult(
		UUID organizationId,
		UUID tenantId,
		OrganizationStatus status) {

	public OrganizationSummaryResult {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
