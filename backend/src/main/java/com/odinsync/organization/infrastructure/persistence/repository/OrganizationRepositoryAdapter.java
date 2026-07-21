package com.odinsync.organization.infrastructure.persistence.repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.odinsync.organization.infrastructure.persistence.exception.OrganizationOptimisticLockException;
import com.odinsync.organization.infrastructure.persistence.exception.OrganizationPersistenceException;
import com.odinsync.organization.infrastructure.persistence.mapper.OrganizationPersistenceMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

	private final SpringDataOrganizationRepository repository;
	private final OrganizationPersistenceMapper mapper;

	public OrganizationRepositoryAdapter(
			SpringDataOrganizationRepository repository,
			OrganizationPersistenceMapper mapper) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public Optional<Organization> findByTenantId(UUID tenantId) {
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		try {
			return repository.findByTenantId(tenantId).map(mapper::toDomain);
		} catch (DataAccessException exception) {
			throw new OrganizationPersistenceException("Unable to load Organization by tenant ID", exception);
		}
	}

	@Override
	public Optional<Organization> findByIdAndTenantId(UUID organizationId, UUID tenantId) {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		try {
			return repository.findByIdAndTenantId(organizationId, tenantId).map(mapper::toDomain);
		} catch (DataAccessException exception) {
			throw new OrganizationPersistenceException("Unable to load Organization by ID and tenant ID", exception);
		}
	}

	@Override
	public boolean existsByTenantId(UUID tenantId) {
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		try {
			return repository.existsByTenantId(tenantId);
		} catch (DataAccessException exception) {
			throw new OrganizationPersistenceException("Unable to check Organization existence by tenant ID", exception);
		}
	}

	@Override
	public void save(Organization organization) {
		Objects.requireNonNull(organization, "organization must not be null");
		try {
			OrganizationJpaEntity entity = repository.findByIdAndTenantId(organization.id(), organization.tenantId())
					.map(existing -> updateExistingEntity(organization, existing))
					.orElseGet(() -> mapper.toNewEntity(organization));
			repository.save(entity);
		} catch (OptimisticLockingFailureException exception) {
			throw new OrganizationOptimisticLockException("Organization was modified by another transaction", exception);
		} catch (DataIntegrityViolationException exception) {
			throw exception;
		} catch (DataAccessException exception) {
			throw new OrganizationPersistenceException("Unable to save Organization", exception);
		}
	}

	private OrganizationJpaEntity updateExistingEntity(Organization organization, OrganizationJpaEntity existing) {
		mapper.updateEntity(organization, existing);
		return existing;
	}
}
