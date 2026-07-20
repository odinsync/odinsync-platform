package com.odinsync.organization.application.service;

import java.util.Objects;

import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationProfileMapper;
import com.odinsync.organization.application.port.in.GetOrganizationProfileUseCase;
import com.odinsync.organization.application.port.out.CurrentTenantProvider;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.query.GetOrganizationProfileQuery;
import com.odinsync.organization.application.result.OrganizationProfileResult;
import com.odinsync.organization.domain.model.Organization;

public class GetOrganizationProfileService implements GetOrganizationProfileUseCase {

	private final OrganizationRepository organizationRepository;
	private final CurrentTenantProvider currentTenantProvider;
	private final OrganizationProfileMapper profileMapper;

	public GetOrganizationProfileService(
			OrganizationRepository organizationRepository,
			CurrentTenantProvider currentTenantProvider,
			OrganizationProfileMapper profileMapper) {
		this.organizationRepository = Objects.requireNonNull(
				organizationRepository,
				"organizationRepository must not be null");
		this.currentTenantProvider = Objects.requireNonNull(
				currentTenantProvider,
				"currentTenantProvider must not be null");
		this.profileMapper = Objects.requireNonNull(profileMapper, "profileMapper must not be null");
	}

	@Override
	public OrganizationProfileResult get(GetOrganizationProfileQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		var tenantId = currentTenantProvider.getCurrentTenantId();
		Organization organization = organizationRepository.findByIdAndTenantId(query.organizationId(), tenantId)
				.orElseThrow(OrganizationNotFoundException::new);
		return profileMapper.toResult(organization);
	}
}
