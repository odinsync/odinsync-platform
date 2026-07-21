package com.odinsync.organization.application.command;

import java.util.Objects;
import java.util.UUID;

public record ProvisionOrganizationCommand(
		UUID tenantId,
		UUID ownerUserId,
		String organizationName,
		String legalName,
		String contactEmail) {

	public ProvisionOrganizationCommand {
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
		Objects.requireNonNull(organizationName, "organizationName must not be null");
		Objects.requireNonNull(legalName, "legalName must not be null");
		Objects.requireNonNull(contactEmail, "contactEmail must not be null");
	}
}
