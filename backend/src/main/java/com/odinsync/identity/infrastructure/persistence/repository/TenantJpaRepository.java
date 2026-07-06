package com.odinsync.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odinsync.identity.infrastructure.persistence.entity.TenantJpaEntity;

import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {

}
