package com.odinsync.organization.infrastructure.persistence.mapper;

import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.CREATED_AT;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.CREATED_BY;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.TENANT_ID;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.UPDATED_AT;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.UPDATED_BY;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.archivedEntity;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.archivedOrganization;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.createdOrganizationWithPendingEvent;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.entity;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.organization;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.suspendedEntity;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.updateTargetEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.domain.model.OrganizationStatus;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import org.junit.jupiter.api.Test;

class OrganizationPersistenceMapperTest {

	private final OrganizationPersistenceMapper mapper = new OrganizationPersistenceMapper();

	@Test
	void mapsDomainToNewEntityWithoutConsumingDomainEvents() {
		Organization organization = createdOrganizationWithPendingEvent();

		OrganizationJpaEntity entity = mapper.toNewEntity(organization);

		assertThat(entity.getId()).isEqualTo(ORGANIZATION_ID);
		assertThat(entity.getTenantId()).isEqualTo(TENANT_ID);
		assertThat(entity.getLegalName()).isEqualTo("Odin Retail Private Limited");
		assertThat(entity.getDisplayName()).isEqualTo("Odin Retail");
		assertThat(entity.getTaxRegistrationNumber()).isEqualTo("TAX-123");
		assertThat(entity.getAddress().getAddressLine1()).isEqualTo("Line 1");
		assertThat(entity.getAddress().getAddressLine2()).isEqualTo("Line 2");
		assertThat(entity.getContact().getEmail()).isEqualTo("owner@odinsync.com");
		assertThat(entity.getContact().getWebsite()).isEqualTo("https://odinsync.com");
		assertThat(entity.getSettings().getCurrencyCode()).isEqualTo("INR");
		assertThat(entity.getSettings().getTimeZone()).isEqualTo("Asia/Kolkata");
		assertThat(entity.getSettings().getLocale()).isEqualTo("en-IN");
		assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(entity.getAudit().getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(entity.getAudit().getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(entity.getVersion()).isZero();
		assertThat(organization.pullDomainEvents()).hasSize(1);
	}

	@Test
	void mapsEntityToDomainWithoutEmittingEvents() {
		Organization organization = mapper.toDomain(entity());

		assertDomainMatchesEntity(organization, entity());
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void roundTripPreservesDomainVisibleState() {
		Organization source = organization();

		Organization reconstituted = mapper.toDomain(mapper.toNewEntity(source));

		assertThat(reconstituted.id()).isEqualTo(source.id());
		assertThat(reconstituted.tenantId()).isEqualTo(source.tenantId());
		assertThat(reconstituted.name()).isEqualTo(source.name());
		assertThat(reconstituted.taxRegistrationNumber()).isEqualTo(source.taxRegistrationNumber());
		assertThat(reconstituted.address()).isEqualTo(source.address());
		assertThat(reconstituted.contact()).isEqualTo(source.contact());
		assertThat(reconstituted.settings()).isEqualTo(source.settings());
		assertThat(reconstituted.status()).isEqualTo(source.status());
		assertThat(reconstituted.auditMetadata()).isEqualTo(source.auditMetadata());
		assertThat(reconstituted.pullDomainEvents()).isEmpty();
	}

	@Test
	void updateEntityCopiesMutableStateAndPreservesIdentityCreationAuditAndVersion() {
		Organization source = organization();
		OrganizationJpaEntity target = updateTargetEntity();
		var originalCreatedAt = target.getAudit().getCreatedAt();
		var originalCreatedBy = target.getAudit().getCreatedBy();
		long originalVersion = target.getVersion();

		mapper.updateEntity(source, target);

		assertThat(target.getId()).isEqualTo(ORGANIZATION_ID);
		assertThat(target.getTenantId()).isEqualTo(TENANT_ID);
		assertThat(target.getLegalName()).isEqualTo(source.name().legalName());
		assertThat(target.getDisplayName()).isEqualTo(source.name().displayName());
		assertThat(target.getAudit().getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(target.getAudit().getCreatedBy()).isEqualTo(originalCreatedBy);
		assertThat(target.getAudit().getUpdatedAt()).isEqualTo(UPDATED_AT);
		assertThat(target.getAudit().getUpdatedBy()).isEqualTo(UPDATED_BY);
		assertThat(target.getVersion()).isEqualTo(originalVersion);
	}

	@Test
	void mapsArchivedAndSuspendedStatusFromPersistence() {
		assertThat(mapper.toDomain(archivedEntity()).status()).isEqualTo(OrganizationStatus.ARCHIVED);
		assertThat(mapper.toDomain(suspendedEntity()).status()).isEqualTo(OrganizationStatus.SUSPENDED);
		assertThat(mapper.toNewEntity(archivedOrganization()).getTaxRegistrationNumber()).isNull();
	}

	@Test
	void rejectsNullArguments() {
		assertThatThrownBy(() -> mapper.toNewEntity(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> mapper.toDomain(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> mapper.updateEntity(null, entity())).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> mapper.updateEntity(organization(), null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsMismatchedIdentityWhenUpdatingExistingEntity() {
		OrganizationJpaEntity entity = updateTargetEntity();
		entity.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));

		assertThatThrownBy(() -> mapper.updateEntity(organization(), entity))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ID");
	}

	@Test
	void rejectsMismatchedTenantWhenUpdatingExistingEntity() {
		OrganizationJpaEntity entity = updateTargetEntity();
		entity.setTenantId(UUID.fromString("88888888-8888-8888-8888-888888888888"));

		assertThatThrownBy(() -> mapper.updateEntity(organization(), entity))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("tenant");
	}

	@Test
	void rejectsMissingRequiredEntityState() {
		OrganizationJpaEntity entity = entity();
		entity.setLegalName(null);

		assertThatThrownBy(() -> mapper.toDomain(entity))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void rejectsInvalidPersistedCurrencyTimeZoneAndEmail() {
		OrganizationJpaEntity invalidCurrency = entity();
		invalidCurrency.getSettings().setCurrencyCode("BAD");
		assertThatThrownBy(() -> mapper.toDomain(invalidCurrency))
				.isInstanceOf(InvalidOrganizationValueException.class);

		OrganizationJpaEntity invalidTimeZone = entity();
		invalidTimeZone.getSettings().setTimeZone("+05:30");
		assertThatThrownBy(() -> mapper.toDomain(invalidTimeZone))
				.isInstanceOf(InvalidOrganizationValueException.class);

		OrganizationJpaEntity invalidEmail = entity();
		invalidEmail.getContact().setEmail("not-an-email");
		assertThatThrownBy(() -> mapper.toDomain(invalidEmail))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	private void assertDomainMatchesEntity(Organization organization, OrganizationJpaEntity entity) {
		assertThat(organization.id()).isEqualTo(entity.getId());
		assertThat(organization.tenantId()).isEqualTo(entity.getTenantId());
		assertThat(organization.name().legalName()).isEqualTo(entity.getLegalName());
		assertThat(organization.name().displayName()).isEqualTo(entity.getDisplayName());
		assertThat(organization.taxRegistrationNumber().value()).contains(entity.getTaxRegistrationNumber());
		assertThat(organization.address().addressLine1()).isEqualTo(entity.getAddress().getAddressLine1());
		assertThat(organization.address().addressLine2Value()).contains(entity.getAddress().getAddressLine2());
		assertThat(organization.contact().email().value()).isEqualTo(entity.getContact().getEmail());
		assertThat(organization.settings().currencyCode().value()).isEqualTo(entity.getSettings().getCurrencyCode());
		assertThat(organization.status()).isEqualTo(entity.getStatus());
		assertThat(organization.auditMetadata().createdAt()).isEqualTo(entity.getAudit().getCreatedAt());
		assertThat(organization.auditMetadata().createdBy()).isEqualTo(entity.getAudit().getCreatedBy());
		assertThat(organization.auditMetadata().updatedAt()).isEqualTo(entity.getAudit().getUpdatedAt());
		assertThat(organization.auditMetadata().updatedBy()).isEqualTo(entity.getAudit().getUpdatedBy());
	}
}
