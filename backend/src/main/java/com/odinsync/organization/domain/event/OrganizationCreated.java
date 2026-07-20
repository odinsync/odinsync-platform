package com.odinsync.organization.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.model.OrganizationStatus;

public record OrganizationCreated(
		UUID eventId,
		UUID organizationId,
		UUID tenantId,
		Instant occurredAt,
		OrganizationStatus status
) implements OrganizationDomainEvent {

	public OrganizationCreated {
		eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		status = Objects.requireNonNull(status, "status must not be null");
	}
}
