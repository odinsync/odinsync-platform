package com.odinsync.organization.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.odinsync.organization.application.command.UpdateOrganizationSettingsCommand;
import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationSettingsMapper;
import com.odinsync.organization.application.model.AuthenticatedActor;
import com.odinsync.organization.application.port.in.UpdateOrganizationSettingsUseCase;
import com.odinsync.organization.application.port.out.CurrentActorProvider;
import com.odinsync.organization.application.port.out.OrganizationAuthorizationService;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.port.out.TimeProvider;
import com.odinsync.organization.application.result.OrganizationSettingsResult;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import com.odinsync.organization.domain.model.Organization;

public class UpdateOrganizationSettingsService implements UpdateOrganizationSettingsUseCase {

	private final OrganizationRepository organizationRepository;
	private final CurrentActorProvider currentActorProvider;
	private final OrganizationAuthorizationService authorizationService;
	private final TimeProvider timeProvider;
	private final OrganizationDomainEventPublisher domainEventPublisher;
	private final OrganizationSettingsMapper settingsMapper;

	public UpdateOrganizationSettingsService(
			OrganizationRepository organizationRepository,
			CurrentActorProvider currentActorProvider,
			OrganizationAuthorizationService authorizationService,
			TimeProvider timeProvider,
			OrganizationDomainEventPublisher domainEventPublisher,
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
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.domainEventPublisher = Objects.requireNonNull(
				domainEventPublisher,
				"domainEventPublisher must not be null");
		this.settingsMapper = Objects.requireNonNull(settingsMapper, "settingsMapper must not be null");
	}

	@Override
	public OrganizationSettingsResult update(UpdateOrganizationSettingsCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		AuthenticatedActor actor = currentActorProvider.getCurrentActor();
		authorizationService.requireSettingsUpdate(actor);
		Organization organization = organizationRepository.findByTenantId(actor.tenantId())
				.orElseThrow(OrganizationNotFoundException::new);
		Instant updatedAt = timeProvider.now();

		organization.updateSettings(command.settings(), updatedAt, actor.userId());

		List<OrganizationDomainEvent> events = organization.pullDomainEvents();
		if (!events.isEmpty()) {
			organizationRepository.save(organization);
			domainEventPublisher.publishAll(events);
		}
		return settingsMapper.toResult(organization);
	}
}
