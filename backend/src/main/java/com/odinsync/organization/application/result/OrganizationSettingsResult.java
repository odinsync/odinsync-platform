package com.odinsync.organization.application.result;

import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.model.OrganizationStatus;
import com.odinsync.organization.domain.valueobject.AuditMetadata;
import com.odinsync.organization.domain.valueobject.OrganizationSettings;

public record OrganizationSettingsResult(
		UUID organizationId,
		UUID tenantId,
		OrganizationSettings settings,
		OrganizationStatus status,
		AuditMetadata auditMetadata) {

	public OrganizationSettingsResult {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		Objects.requireNonNull(settings, "settings must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}
}
