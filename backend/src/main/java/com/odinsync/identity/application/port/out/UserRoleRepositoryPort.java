package com.odinsync.identity.application.port.out;

import com.odinsync.identity.domain.model.Role;

import java.util.UUID;

public interface UserRoleRepositoryPort {

	Role save(Role role);

	void assignRole(UUID userId, UUID roleId);
}
