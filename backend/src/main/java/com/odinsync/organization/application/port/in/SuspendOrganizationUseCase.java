package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.SuspendOrganizationCommand;
import com.odinsync.organization.application.result.OrganizationSummaryResult;

public interface SuspendOrganizationUseCase {

	OrganizationSummaryResult suspend(SuspendOrganizationCommand command);
}
