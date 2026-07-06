package com.odinsync.identity.infrastructure.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.odinsync.identity.domain.model.Tenant;
import com.odinsync.identity.domain.model.SubscriptionPlan;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.infrastructure.persistence.entity.TenantJpaEntity;

@Component
public class TenantPersistenceMapper {

	public TenantJpaEntity toEntity(Tenant tenant) {
		TenantJpaEntity entity = new TenantJpaEntity();
		entity.setId(tenant.id());
		entity.setName(tenant.name());
		entity.setStatus(tenant.status().name());
		entity.setPlan(tenant.plan().name());
		return entity;
	}

	public Tenant toDomain(TenantJpaEntity entity) {
		return new Tenant(
				entity.getId(),
				entity.getName(),
				TenantStatus.valueOf(entity.getStatus()),
				SubscriptionPlan.valueOf(entity.getPlan()));
	}
}
