package com.odinsync.identity.application.port.out;

import com.odinsync.identity.domain.model.Role;

public interface RoleRepositoryPort {

	Role save(Role role);
}
