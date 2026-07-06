package com.odinsync.identity.infrastructure.persistence.mapper;

import java.util.UUID;

import com.odinsync.identity.domain.model.RoleName;
import org.springframework.stereotype.Component;

import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;

@Component
public class RolePersistenceMapper {

	public RoleJpaEntity toEntity(Role role) {
		RoleJpaEntity entity = new RoleJpaEntity();
		entity.setId(role.id());
		entity.setTenantId(role.tenantId());
		entity.setName(role.name().name());
		entity.setDescription(role.description());
		return entity;
	}

	public Role toDomain(RoleJpaEntity entity) {
		return new Role(
				entity.getId(),
				entity.getTenantId(),
				RoleName.valueOf(entity.getName()),
				entity.getDescription());
	}
}
