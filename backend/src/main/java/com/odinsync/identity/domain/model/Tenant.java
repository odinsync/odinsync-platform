package com.odinsync.identity.domain.model;

import java.util.UUID;

public record Tenant(
		UUID id,
		String name,
		TenantStatus status,
		SubscriptionPlan plan
) {

	public static Tenant createFreeTenant(String name) {
		return new Tenant(
				UUID.randomUUID(),
				name,
				TenantStatus.ACTIVE,
				SubscriptionPlan.FREE
		);
	}
}
