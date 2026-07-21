package com.odinsync.organization.application.mapper;

import java.util.Objects;

import com.odinsync.organization.application.result.OrganizationSettingsResult;
import com.odinsync.organization.domain.model.Organization;

public final class OrganizationSettingsMapper {

	public OrganizationSettingsResult toResult(Organization organization) {
		Objects.requireNonNull(organization, "organization must not be null");
		return new OrganizationSettingsResult(
				organization.id(),
				organization.tenantId(),
				organization.settings(),
				organization.status(),
				organization.auditMetadata());
	}
}
