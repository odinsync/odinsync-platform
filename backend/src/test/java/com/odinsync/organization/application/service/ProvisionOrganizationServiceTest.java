package com.odinsync.organization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.application.command.ProvisionOrganizationCommand;
import com.odinsync.organization.application.exception.OrganizationAlreadyExistsException;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.domain.model.OrganizationStatus;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;
import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.CurrencyCode;
import com.odinsync.organization.domain.valueobject.OrganizationSettings;
import com.odinsync.organization.domain.valueobject.OrganizationLocale;
import com.odinsync.organization.domain.valueobject.OrganizationTimeZone;
import com.odinsync.organization.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.Test;

class ProvisionOrganizationServiceTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID OWNER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final Instant CREATED_AT = Instant.parse("2026-07-22T00:00:00Z");

	private final CapturingOrganizationRepository organizationRepository = new CapturingOrganizationRepository();
	private final CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
	private final ProvisionOrganizationService service = new ProvisionOrganizationService(
			organizationRepository,
			() -> ORGANIZATION_ID,
			() -> CREATED_AT,
			eventPublisher,
			defaults());

	@Test
	void provisionsOrganizationAggregateWithConfiguredDefaults() {
		var result = service.provision(command());

		assertThat(result.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(result.tenantId()).isEqualTo(TENANT_ID);
		assertThat(organizationRepository.savedOrganization.id()).isEqualTo(ORGANIZATION_ID);
		assertThat(organizationRepository.savedOrganization.tenantId()).isEqualTo(TENANT_ID);
		assertThat(organizationRepository.savedOrganization.name().displayName()).isEqualTo("Odin Retail");
		assertThat(organizationRepository.savedOrganization.name().legalName()).isEqualTo("Odin Retail Private Limited");
		assertThat(organizationRepository.savedOrganization.contact().email().value()).isEqualTo("owner@odinsync.com");
		assertThat(organizationRepository.savedOrganization.contact().phone()).isEqualTo(defaults().phone());
		assertThat(organizationRepository.savedOrganization.address()).isEqualTo(defaults().address());
		assertThat(organizationRepository.savedOrganization.settings()).isEqualTo(defaults().settings());
		assertThat(organizationRepository.savedOrganization.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organizationRepository.savedOrganization.auditMetadata().createdAt()).isEqualTo(CREATED_AT);
		assertThat(organizationRepository.savedOrganization.auditMetadata().createdBy()).isEqualTo(OWNER_USER_ID);
		assertThat(eventPublisher.publishedEvents).hasSize(1);
	}

	@Test
	void rejectsDuplicateOrganizationForTenant() {
		organizationRepository.exists = true;

		assertThatThrownBy(() -> service.provision(command()))
				.isInstanceOf(OrganizationAlreadyExistsException.class)
				.hasMessageContaining(TENANT_ID.toString());

		assertThat(organizationRepository.savedOrganization).isNull();
		assertThat(eventPublisher.publishedEvents).isEmpty();
	}

	@Test
	void propagatesPersistenceFailureAndDoesNotPublishEvents() {
		RuntimeException failure = new RuntimeException("database unavailable");
		organizationRepository.saveFailure = failure;

		assertThatThrownBy(() -> service.provision(command()))
				.isSameAs(failure);

		assertThat(eventPublisher.publishedEvents).isEmpty();
	}

	@Test
	void invalidConfiguredDefaultsFailFast() {
		assertThatThrownBy(() -> OrganizationProvisioningDefaults.of(
				"1 Main Street",
				null,
				"New York",
				"New York",
				"10001",
				"US",
				"phone",
				"USD",
				"en-US",
				"UTC",
				DateFormat.MM_DD_YYYY.name(),
				TimeFormat.TWENTY_FOUR_HOUR.name(),
				WeekStart.MONDAY.name()))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void blankConfiguredAddressUsesNeutralProvisioningDefaults() {
		OrganizationProvisioningDefaults blankAddressDefaults = OrganizationProvisioningDefaults.of(
				"",
				null,
				"",
				"",
				"",
				"",
				"Not provided",
				"INR",
				"Not provided",
				"Not provided",
				"Not provided",
				"Not provided",
				"Not provided");

		assertThat(blankAddressDefaults.address())
				.isEqualTo(new Address("Not provided", null, "Not provided", "Not provided", "00000", "ZZ"));
		assertThat(blankAddressDefaults.phone()).isEqualTo(new PhoneNumber("0000000000"));
		assertThat(blankAddressDefaults.settings()).isEqualTo(new OrganizationSettings(
				new CurrencyCode("INR"),
				new OrganizationTimeZone("Asia/Kolkata"),
				new OrganizationLocale("en-IN"),
				DateFormat.DD_MM_YYYY,
				TimeFormat.TWENTY_FOUR_HOUR,
				WeekStart.MONDAY));
	}

	private static ProvisionOrganizationCommand command() {
		return new ProvisionOrganizationCommand(
				TENANT_ID,
				OWNER_USER_ID,
				"Odin Retail",
				"Odin Retail Private Limited",
				"owner@odinsync.com");
	}

	private static OrganizationProvisioningDefaults defaults() {
		return new OrganizationProvisioningDefaults(
				new Address("1 Main Street", null, "New York", "New York", "10001", "US"),
				new PhoneNumber("+1 212 555 0100"),
				new OrganizationSettings(
						new CurrencyCode("USD"),
						new OrganizationTimeZone("UTC"),
						new OrganizationLocale("en-US"),
						DateFormat.MM_DD_YYYY,
						TimeFormat.TWENTY_FOUR_HOUR,
						WeekStart.MONDAY));
	}

	private static final class CapturingOrganizationRepository implements OrganizationRepository {
		private boolean exists;
		private RuntimeException saveFailure;
		private Organization savedOrganization;

		@Override
		public Optional<Organization> findByTenantId(UUID tenantId) {
			return Optional.empty();
		}

		@Override
		public Optional<Organization> findByIdAndTenantId(UUID organizationId, UUID tenantId) {
			return Optional.empty();
		}

		@Override
		public boolean existsByTenantId(UUID tenantId) {
			return exists;
		}

		@Override
		public void save(Organization organization) {
			if (saveFailure != null) {
				throw saveFailure;
			}
			savedOrganization = organization;
		}
	}

	private static final class CapturingEventPublisher implements OrganizationDomainEventPublisher {
		private final List<OrganizationDomainEvent> publishedEvents = new ArrayList<>();

		@Override
		public void publishAll(Collection<? extends OrganizationDomainEvent> events) {
			publishedEvents.addAll(events);
		}
	}
}
