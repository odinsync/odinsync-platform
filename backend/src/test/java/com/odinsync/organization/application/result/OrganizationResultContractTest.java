package com.odinsync.organization.application.result;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.address;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.auditMetadata;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.contact;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.name;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.settings;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.taxRegistrationNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.model.OrganizationStatus;
import org.junit.jupiter.api.Test;

class OrganizationResultContractTest {

	@Test
	void createsProfileSettingsAndSummaryResults() {
		OrganizationProfileResult profile = new OrganizationProfileResult(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				OrganizationStatus.ACTIVE,
				auditMetadata());
		OrganizationSettingsResult settingsResult = new OrganizationSettingsResult(
				ORGANIZATION_ID,
				TENANT_ID,
				settings(),
				OrganizationStatus.ACTIVE,
				auditMetadata());
		OrganizationSummaryResult summary =
				new OrganizationSummaryResult(ORGANIZATION_ID, TENANT_ID, OrganizationStatus.ACTIVE);

		assertThat(profile.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(settingsResult.settings()).isEqualTo(settings());
		assertThat(summary.status()).isEqualTo(OrganizationStatus.ACTIVE);
	}

	@Test
	void rejectsNullResultArguments() {
		assertThatThrownBy(() -> new OrganizationSummaryResult(null, TENANT_ID, OrganizationStatus.ACTIVE))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettingsResult(
				ORGANIZATION_ID,
				TENANT_ID,
				null,
				OrganizationStatus.ACTIVE,
				auditMetadata()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationProfileResult(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				null,
				auditMetadata()))
				.isInstanceOf(NullPointerException.class);
	}
}
