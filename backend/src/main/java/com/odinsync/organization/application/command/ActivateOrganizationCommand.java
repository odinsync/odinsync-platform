package com.odinsync.organization.application.command;

import java.util.Objects;
import java.util.UUID;

public record ActivateOrganizationCommand(UUID organizationId) {

	public ActivateOrganizationCommand {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
	}
}
