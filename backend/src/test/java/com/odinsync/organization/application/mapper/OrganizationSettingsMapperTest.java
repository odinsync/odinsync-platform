package com.odinsync.organization.application.mapper;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.organization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.application.result.OrganizationSettingsResult;
import org.junit.jupiter.api.Test;

class OrganizationSettingsMapperTest {

	private final OrganizationSettingsMapper mapper = new OrganizationSettingsMapper();

	@Test
	void mapsOrganizationToSettingsResult() {
		OrganizationSettingsResult result = mapper.toResult(organization());

		assertThat(result.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(result.tenantId()).isEqualTo(TENANT_ID);
		assertThat(result.settings()).isEqualTo(organization().settings());
		assertThat(result.status()).isEqualTo(organization().status());
		assertThat(result.auditMetadata()).isEqualTo(organization().auditMetadata());
	}

	@Test
	void rejectsNullOrganization() {
		assertThatThrownBy(() -> mapper.toResult(null))
				.isInstanceOf(NullPointerException.class);
	}
}
