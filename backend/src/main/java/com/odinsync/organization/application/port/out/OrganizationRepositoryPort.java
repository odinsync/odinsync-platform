package com.odinsync.organization.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.domain.model.Organization;

public interface OrganizationRepositoryPort {

	Optional<Organization> findByTenantId(UUID tenantId);

	Optional<Organization> findByIdAndTenantId(UUID organizationId, UUID tenantId);

	boolean existsByTenantId(UUID tenantId);

	Organization save(Organization organization);
}
