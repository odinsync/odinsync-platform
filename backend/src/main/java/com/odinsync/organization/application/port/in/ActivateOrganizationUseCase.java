package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.ActivateOrganizationCommand;
import com.odinsync.organization.application.result.OrganizationSummaryResult;

public interface ActivateOrganizationUseCase {

	OrganizationSummaryResult activate(ActivateOrganizationCommand command);
}
