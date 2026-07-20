package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrganizationReconstitutionTest {

	@Test
	void reconstitutesActiveSuspendedAndArchivedOrganizationsWithoutEvents() {
		for (OrganizationStatus status : OrganizationStatus.values()) {
			Organization organization = OrganizationTestFixtures.reconstituted(status);

			assertThat(organization.id()).isEqualTo(ORGANIZATION_ID);
			assertThat(organization.tenantId()).isEqualTo(TENANT_ID);
			assertThat(organization.name()).isEqualTo(OrganizationTestFixtures.name());
			assertThat(organization.taxRegistrationNumber()).isEqualTo(OrganizationTestFixtures.taxRegistrationNumber());
			assertThat(organization.address()).isEqualTo(OrganizationTestFixtures.address());
			assertThat(organization.contact()).isEqualTo(OrganizationTestFixtures.contact());
			assertThat(organization.settings()).isEqualTo(OrganizationTestFixtures.settings());
			assertThat(organization.status()).isEqualTo(status);
			assertThat(organization.auditMetadata()).isEqualTo(OrganizationTestFixtures.auditMetadata());
			assertThat(organization.pullDomainEvents()).isEmpty();
		}
	}

	@Test
	void rejectsNullReconstitutionArguments() {
		assertThatThrownBy(() -> Organization.reconstitute(null, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), OrganizationStatus.ACTIVE, OrganizationTestFixtures.auditMetadata()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.reconstitute(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), null, OrganizationTestFixtures.auditMetadata()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Organization.reconstitute(ORGANIZATION_ID, TENANT_ID, OrganizationTestFixtures.name(), OrganizationTestFixtures.taxRegistrationNumber(), OrganizationTestFixtures.address(), OrganizationTestFixtures.contact(), OrganizationTestFixtures.settings(), OrganizationStatus.ACTIVE, null))
				.isInstanceOf(NullPointerException.class);
	}
}
