package com.odinsync.organization.application.mapper;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.organization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.application.result.OrganizationProfileResult;
import org.junit.jupiter.api.Test;

class OrganizationProfileMapperTest {

	private final OrganizationProfileMapper mapper = new OrganizationProfileMapper();

	@Test
	void mapsOrganizationToProfileResult() {
		OrganizationProfileResult result = mapper.toResult(organization());

		assertThat(result.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(result.tenantId()).isEqualTo(TENANT_ID);
		assertThat(result.name()).isEqualTo(organization().name());
		assertThat(result.taxRegistrationNumber()).isEqualTo(organization().taxRegistrationNumber());
		assertThat(result.address()).isEqualTo(organization().address());
		assertThat(result.contact()).isEqualTo(organization().contact());
		assertThat(result.status()).isEqualTo(organization().status());
		assertThat(result.auditMetadata()).isEqualTo(organization().auditMetadata());
	}

	@Test
	void rejectsNullOrganization() {
		assertThatThrownBy(() -> mapper.toResult(null))
				.isInstanceOf(NullPointerException.class);
	}
}
