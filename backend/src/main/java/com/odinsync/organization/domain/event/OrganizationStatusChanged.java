package com.odinsync.organization.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.exception.OrganizationStateConflictException;
import com.odinsync.organization.domain.model.OrganizationStatus;

public record OrganizationStatusChanged(
		UUID eventId,
		UUID organizationId,
		UUID tenantId,
		Instant occurredAt,
		UUID changedBy,
		OrganizationStatus previousStatus,
		OrganizationStatus currentStatus
) implements OrganizationDomainEvent {

	public OrganizationStatusChanged {
		eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
		previousStatus = Objects.requireNonNull(previousStatus, "previousStatus must not be null");
		currentStatus = Objects.requireNonNull(currentStatus, "currentStatus must not be null");
		if (previousStatus == currentStatus) {
			throw new OrganizationStateConflictException("Status change must change the current status");
		}
	}
}
