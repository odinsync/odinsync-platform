package com.odinsync.identity.infrastructure.persistence.adapter;

import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.odinsync.identity.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RolePersistenceAdapter implements RoleRepositoryPort {

	private final RoleJpaRepository roleJpaRepository;
	private final RolePersistenceMapper mapper;

	@Override
	public Role save(Role role) {
		RoleJpaEntity saved = roleJpaRepository.save(mapper.toEntity(role));
		return mapper.toDomain(saved);
	}
}
