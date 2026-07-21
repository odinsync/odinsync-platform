package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.ProvisionOrganizationCommand;
import com.odinsync.organization.application.result.ProvisionedOrganizationResult;

public interface ProvisionOrganizationUseCase {

	ProvisionedOrganizationResult provision(ProvisionOrganizationCommand command);
}
