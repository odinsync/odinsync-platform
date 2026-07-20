package com.odinsync.organization.application.command;

import java.util.Objects;
import java.util.UUID;

public record SuspendOrganizationCommand(UUID organizationId) {

	public SuspendOrganizationCommand {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
	}
}
