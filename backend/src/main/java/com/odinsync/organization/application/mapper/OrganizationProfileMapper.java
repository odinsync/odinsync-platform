package com.odinsync.organization.application.mapper;

import java.util.Objects;

import com.odinsync.organization.application.result.OrganizationProfileResult;
import com.odinsync.organization.domain.model.Organization;

public final class OrganizationProfileMapper {

	public OrganizationProfileResult toResult(Organization organization) {
		Objects.requireNonNull(organization, "organization must not be null");
		return new OrganizationProfileResult(
				organization.id(),
				organization.tenantId(),
				organization.name(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				organization.status(),
				organization.auditMetadata());
	}
}
