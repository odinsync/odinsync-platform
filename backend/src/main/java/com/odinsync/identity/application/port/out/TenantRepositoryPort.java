package com.odinsync.identity.application.port.out;

import com.odinsync.identity.domain.model.Tenant;

public interface TenantRepositoryPort {

	Tenant save(Tenant tenant);
}
