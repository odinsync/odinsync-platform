package com.odinsync.organization.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrganizationRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

	Optional<OrganizationJpaEntity> findByTenantId(UUID tenantId);

	Optional<OrganizationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

	boolean existsByTenantId(UUID tenantId);
}
