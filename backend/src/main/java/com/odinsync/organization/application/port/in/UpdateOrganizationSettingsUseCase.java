package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.UpdateOrganizationSettingsCommand;
import com.odinsync.organization.application.result.OrganizationSettingsResult;

public interface UpdateOrganizationSettingsUseCase {

	OrganizationSettingsResult update(UpdateOrganizationSettingsCommand command);
}
