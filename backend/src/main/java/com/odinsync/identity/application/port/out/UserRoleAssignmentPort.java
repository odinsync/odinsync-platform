package com.odinsync.identity.application.port.out;

import java.util.UUID;

public interface UserRoleAssignmentPort {

	void assignRole(UUID userId, UUID roleId);
}
