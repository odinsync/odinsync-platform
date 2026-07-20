package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.UpdateOrganizationProfileCommand;
import com.odinsync.organization.application.result.OrganizationProfileResult;

public interface UpdateOrganizationProfileUseCase {

	OrganizationProfileResult update(UpdateOrganizationProfileCommand command);
}
