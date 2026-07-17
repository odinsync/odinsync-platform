package com.odinsync.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odinsync.identity.infrastructure.persistence.entity.UserJpaEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

	boolean existsByEmail(String email);

	Optional<UserJpaEntity> findByEmail(String email);
}
