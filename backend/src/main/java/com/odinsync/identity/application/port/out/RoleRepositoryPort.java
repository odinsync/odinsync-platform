package com.odinsync.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.domain.model.Role;

public interface RoleRepositoryPort {

	Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

	Role save(Role role);
}
