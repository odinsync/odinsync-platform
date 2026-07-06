package com.odinsync.identity.infrastructure.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.odinsync.identity.domain.model.User;
import com.odinsync.identity.domain.model.UserStatus;
import com.odinsync.identity.infrastructure.persistence.entity.UserJpaEntity;

@Component
public class UserPersistenceMapper {

	public UserJpaEntity toEntity(User user) {
		UserJpaEntity entity = new UserJpaEntity();
		entity.setId(user.id());
		entity.setTenantId(user.tenantId());
		entity.setFullName(user.fullName());
		entity.setEmail(user.email());
		entity.setPasswordHash(user.passwordHash());
		entity.setStatus(user.status().name());
		return entity;
	}

	public User toDomain(UserJpaEntity entity) {
		return new User(
				entity.getId(),
				entity.getTenantId(),
				entity.getFullName(),
				entity.getEmail(),
				entity.getPasswordHash(),
				UserStatus.valueOf(entity.getStatus()));
	}
}
