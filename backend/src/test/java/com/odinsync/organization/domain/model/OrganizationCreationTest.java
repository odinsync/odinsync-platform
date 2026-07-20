package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.ACTOR_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.CREATED_AT;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.odinsync.organization.domain.event.OrganizationCreated;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import org.junit.jupiter.api.Test;

class OrganizationCreationTest {

	@Test
	void createsActiveOrganizationWithInitialAuditMetadataAndCreationEvent() {
		Organization organization = OrganizationTestFixtures.newOrganization();

		assertThat(organization.id()).isEqualTo(ORGANIZATION_ID);
		assertThat(organization.tenantId()).isEqualTo(TENANT_ID);
		assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organization.auditMetadata().createdAt()).isEqualTo(CREATED_AT);
		assertThat(organization.auditMetadata().updatedAt()).isEqualTo(CREATED_AT);
		assertThat(organization.auditMetadata().createdBy()).isEqualTo(ACTOR_ID);
		assertThat(organization.auditMetadata().updatedBy()).isEqualTo(ACTOR_ID);

		List<OrganizationDomainEvent> events = organization.pullDomainEvents();
		assertThat(events).hasSize(1);
		OrganizationCreated event = (OrganizationCreated) events.getFirst();
		assertThat(event.eventId()).isNotNull();
		assertThat(event.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(event.tenantId()).isEqualTo(TENANT_ID);
		assertThat(event.occurredAt()).isEqualTo(CREATED_AT);
		assertThat(event.status()).isEqualTo(OrganizationStatus.ACTIVE);
	}

	@Test
	void rejectsNullCreationArguments() {
		assertThatThrownBy(() -> Organization.create(null, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, null, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, null, OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), null, OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), null, OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), null, OrganizationTestFixtures.settings(), CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), null, CREATED_AT, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), null, ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.create(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), CREATED_AT, null))
				.isInstanceOf(NullPointerException.class);
	}
}
