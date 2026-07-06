package com.odinsync.identity.infrastructure.persistence.adapter;

import java.util.UUID;

import com.odinsync.identity.application.port.out.UserRoleAssignmentPort;
import com.odinsync.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRoleAssignmentAdapter implements UserRoleAssignmentPort {

	private final UserRoleJpaRepository userRoleJpaRepository;

	@Override
	public void assignRole(UUID userId, UUID roleId) {
		UserRoleJpaEntity entity = new UserRoleJpaEntity();
		entity.setUserId(userId);
		entity.setRoleId(roleId);
		userRoleJpaRepository.save(entity);
	}
}
