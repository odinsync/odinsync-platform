package com.odinsync.identity.infrastructure.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.odinsync.identity.domain.model.Organization;
import com.odinsync.identity.infrastructure.persistence.entity.OrganizationJpaEntity;

@Component
public class OrganizationPersistenceMapper {

	public OrganizationJpaEntity toEntity(Organization organization) {
		OrganizationJpaEntity entity = new OrganizationJpaEntity();
		entity.setId(organization.id());
		entity.setTenantId(organization.tenantId());
		entity.setName(organization.name());
		entity.setLegalName(organization.legalName());
		entity.setEmail(organization.email());
		return entity;
	}

	public Organization toDomain(OrganizationJpaEntity entity) {
		return new Organization(
				entity.getId(),
				entity.getTenantId(),
				entity.getName(),
				entity.getLegalName(),
				entity.getEmail());
	}
}
