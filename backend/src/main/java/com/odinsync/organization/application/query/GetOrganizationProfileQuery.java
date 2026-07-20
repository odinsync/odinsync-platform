package com.odinsync.organization.application.query;

import java.util.Objects;
import java.util.UUID;

public record GetOrganizationProfileQuery(UUID organizationId) {

	public GetOrganizationProfileQuery {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
	}
}
