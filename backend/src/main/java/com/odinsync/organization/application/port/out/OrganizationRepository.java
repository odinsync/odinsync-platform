package com.odinsync.organization.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.domain.model.Organization;

public interface OrganizationRepository {

	Optional<Organization> findByTenantId(UUID tenantId);

	Optional<Organization> findByIdAndTenantId(UUID organizationId, UUID tenantId);

	boolean existsByTenantId(UUID tenantId);

	void save(Organization organization);
}
