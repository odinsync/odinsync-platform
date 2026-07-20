package com.odinsync.organization.application.command;

import java.util.Objects;
import java.util.UUID;

public record ArchiveOrganizationCommand(UUID organizationId) {

	public ArchiveOrganizationCommand {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
	}
}
