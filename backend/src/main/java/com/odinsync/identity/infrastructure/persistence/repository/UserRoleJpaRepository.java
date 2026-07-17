package com.odinsync.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleJpaEntity.UserRoleId> {

	@Query("""
			select role
			from RoleJpaEntity role
			join UserRoleJpaEntity userRole on userRole.roleId = role.id
			where userRole.userId = :userId
			""")
	List<RoleJpaEntity> findRolesByUserId(@Param("userId") UUID userId);
}
