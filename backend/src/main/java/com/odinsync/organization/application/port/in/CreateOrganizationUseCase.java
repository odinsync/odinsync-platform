package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.CreateOrganizationCommand;
import com.odinsync.organization.application.result.OrganizationSummaryResult;

public interface CreateOrganizationUseCase {

	OrganizationSummaryResult create(CreateOrganizationCommand command);
}
