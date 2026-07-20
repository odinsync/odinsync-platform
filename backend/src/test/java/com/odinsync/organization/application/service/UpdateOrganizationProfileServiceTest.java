package com.odinsync.organization.application.service;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ACTOR_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.UPDATED_AT;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.archivedOrganization;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.actor;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.changedName;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.organization;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.updateProfileCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.odinsync.organization.application.command.UpdateOrganizationProfileCommand;
import com.odinsync.organization.application.exception.MissingActorContextException;
import com.odinsync.organization.application.exception.MissingTenantContextException;
import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationProfileMapper;
import com.odinsync.organization.application.port.out.CurrentActorProvider;
import com.odinsync.organization.application.port.out.CurrentTenantProvider;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.port.out.TimeProvider;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import com.odinsync.organization.domain.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateOrganizationProfileServiceTest {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private CurrentTenantProvider currentTenantProvider;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@Mock
	private TimeProvider timeProvider;

	@Mock
	private OrganizationDomainEventPublisher domainEventPublisher;

	private final OrganizationProfileMapper profileMapper = new OrganizationProfileMapper();

	private UpdateOrganizationProfileService service;

	@BeforeEach
	void setUp() {
		service = new UpdateOrganizationProfileService(
				organizationRepository,
				currentTenantProvider,
				currentActorProvider,
				timeProvider,
				domainEventPublisher,
				profileMapper);
	}

	@Test
	void updatesProfileForAuthenticatedTenantAndPublishesEventsAfterSave() {
		Organization organization = organization();
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));

		var result = service.update(updateProfileCommand());

		assertThat(result.name()).isEqualTo(updateProfileCommand().name());
		assertThat(result.auditMetadata().updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(result.auditMetadata().updatedBy()).isEqualTo(ACTOR_ID);
		verify(organizationRepository).findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID);

		InOrder inOrder = inOrder(organizationRepository, domainEventPublisher);
		inOrder.verify(organizationRepository).save(organization);
		inOrder.verify(domainEventPublisher).publishAll(any());
	}

	@Test
	void persistenceFailurePreventsEventPublication() {
		Organization organization = organization();
		RuntimeException persistenceFailure = new RuntimeException("database unavailable");
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));
		doThrow(persistenceFailure).when(organizationRepository).save(organization);

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isSameAs(persistenceFailure);

		verifyNoInteractions(domainEventPublisher);
	}

	@Test
	void tenantProviderFailurePreventsRepositoryAccess() {
		MissingTenantContextException missingTenant = new MissingTenantContextException();
		when(currentTenantProvider.getCurrentTenantId()).thenThrow(missingTenant);

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isSameAs(missingTenant);

		verifyNoInteractions(currentActorProvider, organizationRepository, timeProvider, domainEventPublisher);
	}

	@Test
	void actorProviderFailurePreventsRepositoryAccess() {
		MissingActorContextException missingActor = new MissingActorContextException();
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenThrow(missingActor);

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isSameAs(missingActor);

		verifyNoInteractions(organizationRepository, timeProvider, domainEventPublisher);
	}

	@Test
	void timeProviderFailurePreventsAggregateMutationAndPersistence() {
		RuntimeException clockFailure = new RuntimeException("clock unavailable");
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenThrow(clockFailure);

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isSameAs(clockFailure);

		verifyNoInteractions(organizationRepository, domainEventPublisher);
	}

	@Test
	void throwsWhenOrganizationIsMissing() {
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isInstanceOf(OrganizationNotFoundException.class);

		verifyNoInteractions(domainEventPublisher);
		verify(organizationRepository, never()).save(any(Organization.class));
	}

	@Test
	void doesNotPublishWhenProfileUpdateProducesNoEvents() {
		Organization organization = organization();
		var auditMetadata = organization.auditMetadata();
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));

		var result = service.update(new UpdateOrganizationProfileCommand(
				ORGANIZATION_ID,
				organization.name(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact()));

		assertThat(result.auditMetadata()).isEqualTo(auditMetadata);
		verify(organizationRepository, never()).save(organization);
		verify(domainEventPublisher, never()).publishAll(any());
	}

	@Test
	void partialProfileChangeUpdatesOnlyChangedValueAndPublishesOneEvent() {
		Organization organization = organization();
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));

		var result = service.update(new UpdateOrganizationProfileCommand(
				ORGANIZATION_ID,
				changedName(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact()));

		assertThat(result.name()).isEqualTo(changedName());
		assertThat(result.address()).isEqualTo(organization.address());
		verify(organizationRepository).save(organization);
		verify(domainEventPublisher).publishAll(argThat(events -> events.size() == 1));
	}

	@Test
	void archivedOrganizationDomainExceptionPreventsSaveAndPublication() {
		Organization organization = archivedOrganization();
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isInstanceOf(ArchivedOrganizationModificationException.class);

		verify(organizationRepository, never()).save(any(Organization.class));
		verifyNoInteractions(domainEventPublisher);
	}

	@Test
	void publisherFailurePropagatesAfterSave() {
		Organization organization = organization();
		RuntimeException publisherFailure = new RuntimeException("publisher unavailable");
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(timeProvider.now()).thenReturn(UPDATED_AT);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization));
		doThrow(publisherFailure).when(domainEventPublisher).publishAll(any());

		assertThatThrownBy(() -> service.update(updateProfileCommand()))
				.isSameAs(publisherFailure);

		verify(organizationRepository).save(organization);
	}

	@Test
	void rejectsNullCommand() {
		assertThatThrownBy(() -> service.update(null))
				.isInstanceOf(NullPointerException.class);
	}
}
