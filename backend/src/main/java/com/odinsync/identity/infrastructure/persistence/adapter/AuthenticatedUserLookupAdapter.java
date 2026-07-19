package com.odinsync.identity.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.port.out.AuthenticatedUserLookupPort;
import com.odinsync.identity.application.usecase.AuthenticatedUser;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.TenantJpaRepository;
import com.odinsync.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.odinsync.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthenticatedUserLookupAdapter implements AuthenticatedUserLookupPort {

	private final UserJpaRepository userRepository;
	private final TenantJpaRepository tenantRepository;
	private final UserRoleJpaRepository userRoleRepository;

	@Override
	public Optional<AuthenticatedUser> findById(UUID userId) {
		return userRepository.findById(userId)
				.flatMap(this::toAuthenticatedUser);
	}

	private Optional<AuthenticatedUser> toAuthenticatedUser(UserJpaEntity user) {
		return tenantRepository.findById(user.getTenantId())
				.map(tenant -> new AuthenticatedUser(
						user.getId(),
						user.getTenantId(),
						user.getEmail(),
						userRoleRepository.findRolesByUserId(user.getId())
								.stream()
								.map(RoleJpaEntity::getName)
								.toList(),
						UserStatus.valueOf(user.getStatus()),
						TenantStatus.valueOf(tenant.getStatus())));
	}
}
