package com.odinsync.organization.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrganizationDomainEvent
		permits OrganizationCreated,
				OrganizationProfileUpdated,
				OrganizationSettingsUpdated,
				OrganizationStatusChanged {

	UUID eventId();

	UUID organizationId();

	UUID tenantId();

	Instant occurredAt();
}
