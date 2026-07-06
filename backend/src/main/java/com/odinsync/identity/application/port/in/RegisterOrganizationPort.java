package com.odinsync.identity.application.port.in;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.usecase.RegisterOrganizationResult;

public interface RegisterOrganizationPort {
	RegisterOrganizationResult register(RegisterOrganizationCommand command);
}
