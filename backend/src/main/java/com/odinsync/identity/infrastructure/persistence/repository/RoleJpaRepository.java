package com.odinsync.identity.infrastructure.persistence.repository;

import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {
}