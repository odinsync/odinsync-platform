package com.odinsync.identity.infrastructure.persistence.adapter;

import com.odinsync.identity.infrastructure.persistence.entity.TenantJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.domain.model.Tenant;
import com.odinsync.identity.infrastructure.persistence.mapper.TenantPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class TenantPersistenceAdapter implements TenantRepositoryPort {
	private final TenantJpaRepository repository;
	private final TenantPersistenceMapper mapper;

	@Override
	public Tenant save(Tenant tenant) {
		TenantJpaEntity saved = repository.save(mapper.toEntity(tenant));
		return mapper.toDomain(saved);
	}
}
