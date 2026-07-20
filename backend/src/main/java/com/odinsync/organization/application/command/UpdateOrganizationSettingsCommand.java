package com.odinsync.organization.application.command;

import java.util.Objects;

import com.odinsync.organization.domain.valueobject.OrganizationSettings;

public record UpdateOrganizationSettingsCommand(OrganizationSettings settings) {

	public UpdateOrganizationSettingsCommand {
		Objects.requireNonNull(settings, "settings must not be null");
	}
}
