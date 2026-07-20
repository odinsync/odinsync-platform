package com.odinsync.organization.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrganizationProfileUpdated(
		UUID eventId,
		UUID organizationId,
		UUID tenantId,
		Instant occurredAt,
		UUID changedBy
) implements OrganizationDomainEvent {

	public OrganizationProfileUpdated {
		eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
	}
}
