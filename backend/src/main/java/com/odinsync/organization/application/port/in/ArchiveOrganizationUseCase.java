package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.command.ArchiveOrganizationCommand;
import com.odinsync.organization.application.result.OrganizationSummaryResult;

public interface ArchiveOrganizationUseCase {

	OrganizationSummaryResult archive(ArchiveOrganizationCommand command);
}
