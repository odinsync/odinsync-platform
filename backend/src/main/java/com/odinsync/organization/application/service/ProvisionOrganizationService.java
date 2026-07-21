package com.odinsync.organization.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.application.command.ProvisionOrganizationCommand;
import com.odinsync.organization.application.exception.OrganizationAlreadyExistsException;
import com.odinsync.organization.application.port.in.ProvisionOrganizationUseCase;
import com.odinsync.organization.application.port.out.IdGenerator;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.port.out.TimeProvider;
import com.odinsync.organization.application.result.ProvisionedOrganizationResult;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.domain.valueobject.EmailAddress;
import com.odinsync.organization.domain.valueobject.OrganizationContact;
import com.odinsync.organization.domain.valueobject.OrganizationName;
import com.odinsync.organization.domain.valueobject.TaxRegistrationNumber;
import com.odinsync.organization.domain.valueobject.Website;

public class ProvisionOrganizationService implements ProvisionOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final IdGenerator idGenerator;
	private final TimeProvider timeProvider;
	private final OrganizationDomainEventPublisher domainEventPublisher;
	private final OrganizationProvisioningDefaults defaults;

	public ProvisionOrganizationService(
			OrganizationRepository organizationRepository,
			IdGenerator idGenerator,
			TimeProvider timeProvider,
			OrganizationDomainEventPublisher domainEventPublisher,
			OrganizationProvisioningDefaults defaults) {
		this.organizationRepository = Objects.requireNonNull(
				organizationRepository,
				"organizationRepository must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.domainEventPublisher = Objects.requireNonNull(
				domainEventPublisher,
				"domainEventPublisher must not be null");
		this.defaults = Objects.requireNonNull(defaults, "defaults must not be null");
	}

	@Override
	public ProvisionedOrganizationResult provision(ProvisionOrganizationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		if (organizationRepository.existsByTenantId(command.tenantId())) {
			throw new OrganizationAlreadyExistsException(command.tenantId());
		}

		UUID organizationId = idGenerator.generateId();
		Instant createdAt = timeProvider.now();
		Organization organization = Organization.create(
				organizationId,
				command.tenantId(),
				new OrganizationName(command.legalName(), command.organizationName()),
				TaxRegistrationNumber.empty(),
				defaults.address(),
				new OrganizationContact(
						new EmailAddress(command.contactEmail()),
						defaults.phone(),
						Website.empty()),
				defaults.settings(),
				createdAt,
				command.ownerUserId());

		List<OrganizationDomainEvent> events = organization.pullDomainEvents();
		organizationRepository.save(organization);
		if (!events.isEmpty()) {
			domainEventPublisher.publishAll(events);
		}
		return new ProvisionedOrganizationResult(organization.id(), organization.tenantId());
	}
}
