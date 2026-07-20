package com.odinsync.organization.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.odinsync.organization.application.command.UpdateOrganizationProfileCommand;
import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationProfileMapper;
import com.odinsync.organization.application.model.AuthenticatedActor;
import com.odinsync.organization.application.port.in.UpdateOrganizationProfileUseCase;
import com.odinsync.organization.application.port.out.CurrentActorProvider;
import com.odinsync.organization.application.port.out.CurrentTenantProvider;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.port.out.TimeProvider;
import com.odinsync.organization.application.result.OrganizationProfileResult;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import com.odinsync.organization.domain.model.Organization;

public class UpdateOrganizationProfileService implements UpdateOrganizationProfileUseCase {

	private final OrganizationRepository organizationRepository;
	private final CurrentTenantProvider currentTenantProvider;
	private final CurrentActorProvider currentActorProvider;
	private final TimeProvider timeProvider;
	private final OrganizationDomainEventPublisher domainEventPublisher;
	private final OrganizationProfileMapper profileMapper;

	public UpdateOrganizationProfileService(
			OrganizationRepository organizationRepository,
			CurrentTenantProvider currentTenantProvider,
			CurrentActorProvider currentActorProvider,
			TimeProvider timeProvider,
			OrganizationDomainEventPublisher domainEventPublisher,
			OrganizationProfileMapper profileMapper) {
		this.organizationRepository = Objects.requireNonNull(
				organizationRepository,
				"organizationRepository must not be null");
		this.currentTenantProvider = Objects.requireNonNull(
				currentTenantProvider,
				"currentTenantProvider must not be null");
		this.currentActorProvider = Objects.requireNonNull(
				currentActorProvider,
				"currentActorProvider must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.domainEventPublisher = Objects.requireNonNull(
				domainEventPublisher,
				"domainEventPublisher must not be null");
		this.profileMapper = Objects.requireNonNull(profileMapper, "profileMapper must not be null");
	}

	@Override
	public OrganizationProfileResult update(UpdateOrganizationProfileCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		var tenantId = currentTenantProvider.getCurrentTenantId();
		AuthenticatedActor actor = currentActorProvider.getCurrentActor();
		Instant updatedAt = timeProvider.now();
		Organization organization = organizationRepository.findByIdAndTenantId(command.organizationId(), tenantId)
				.orElseThrow(OrganizationNotFoundException::new);

		organization.updateProfile(
				command.name(),
				command.taxRegistrationNumber(),
				command.address(),
				command.contact(),
				updatedAt,
				actor.userId());

		List<OrganizationDomainEvent> events = organization.pullDomainEvents();
		if (!events.isEmpty()) {
			organizationRepository.save(organization);
			domainEventPublisher.publishAll(events);
		}
		return profileMapper.toResult(organization);
	}
}
