package com.odinsync.organization.application.service;

import java.util.Objects;

import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationSettingsMapper;
import com.odinsync.organization.application.model.AuthenticatedActor;
import com.odinsync.organization.application.port.in.GetOrganizationSettingsUseCase;
import com.odinsync.organization.application.port.out.CurrentActorProvider;
import com.odinsync.organization.application.port.out.OrganizationAuthorizationService;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.query.GetOrganizationSettingsQuery;
import com.odinsync.organization.application.result.OrganizationSettingsResult;
import com.odinsync.organization.domain.model.Organization;

public class GetOrganizationSettingsService implements GetOrganizationSettingsUseCase {

	private final OrganizationRepository organizationRepository;
	private final CurrentActorProvider currentActorProvider;
	private final OrganizationAuthorizationService authorizationService;
	private final OrganizationSettingsMapper settingsMapper;

	public GetOrganizationSettingsService(
			OrganizationRepository organizationRepository,
			CurrentActorProvider currentActorProvider,
			OrganizationAuthorizationService authorizationService,
			OrganizationSettingsMapper settingsMapper) {
		this.organizationRepository = Objects.requireNonNull(
				organizationRepository,
				"organizationRepository must not be null");
		this.currentActorProvider = Objects.requireNonNull(
				currentActorProvider,
				"currentActorProvider must not be null");
		this.authorizationService = Objects.requireNonNull(
				authorizationService,
				"authorizationService must not be null");
		this.settingsMapper = Objects.requireNonNull(settingsMapper, "settingsMapper must not be null");
	}

	@Override
	public OrganizationSettingsResult get(GetOrganizationSettingsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		AuthenticatedActor actor = currentActorProvider.getCurrentActor();
		authorizationService.requireSettingsRead(actor);
		Organization organization = organizationRepository.findByTenantId(actor.tenantId())
				.orElseThrow(OrganizationNotFoundException::new);
		return settingsMapper.toResult(organization);
	}
}
