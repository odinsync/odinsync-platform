package com.odinsync.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odinsync.identity.infrastructure.persistence.entity.OrganizationJpaEntity;

import java.util.UUID;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

}