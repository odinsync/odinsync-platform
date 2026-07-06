package com.odinsync.identity.infrastructure.persistence.adapter;

import java.util.UUID;

import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.odinsync.identity.infrastructure.persistence.repository.RoleJpaRepository;
import com.odinsync.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import com.odinsync.identity.application.port.out.UserRoleRepositoryPort;
import com.odinsync.identity.infrastructure.persistence.entity.UserRoleJpaEntity;

@Repository
@RequiredArgsConstructor
public class RolePersistenceAdapter implements UserRoleRepositoryPort {

	private final UserRoleJpaRepository userRoleJpaRepository;
	private final RoleJpaRepository roleJpaRepository;
	private final RolePersistenceMapper mapper;
	@Override
	public void assignRole(UUID userId, UUID roleId) {
		UserRoleJpaEntity entity = new UserRoleJpaEntity();
		entity.setUserId(userId);
		entity.setRoleId(roleId);
		userRoleJpaRepository.save(entity);
	}

	@Override
	public Role save(Role role) {
		RoleJpaEntity saved = roleJpaRepository.save(mapper.toEntity(role));
		return mapper.toDomain(saved);
	}
}
