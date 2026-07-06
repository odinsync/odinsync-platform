package com.odinsync.identity.infrastructure.persistence.adapter;

import com.odinsync.identity.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.OrganizationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.odinsync.identity.application.port.out.OrganizationRepositoryPort;
import com.odinsync.identity.domain.model.Organization;
import com.odinsync.identity.infrastructure.persistence.mapper.OrganizationPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class OrganizationPersistenceAdapter implements OrganizationRepositoryPort {

	private final OrganizationJpaRepository repository;
	private final OrganizationPersistenceMapper mapper;

	@Override
	public Organization save(Organization organization) {
		OrganizationJpaEntity saved = repository.save(mapper.toEntity(organization));
		return mapper.toDomain(saved);
	}
}
