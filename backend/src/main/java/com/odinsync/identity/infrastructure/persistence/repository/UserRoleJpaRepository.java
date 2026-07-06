package com.odinsync.identity.infrastructure.persistence.repository;

import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.odinsync.identity.infrastructure.persistence.entity.UserRoleJpaEntity;

import java.util.Optional;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleJpaEntity.UserRoleId> {
}
