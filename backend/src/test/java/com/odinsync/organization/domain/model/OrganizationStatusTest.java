package com.odinsync.organization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationStatusTest {

	@Test
	void exposesExactStableValues() {
		assertThat(OrganizationStatus.values()).containsExactly(
				OrganizationStatus.ACTIVE,
				OrganizationStatus.SUSPENDED,
				OrganizationStatus.ARCHIVED);
		assertThat(OrganizationStatus.ACTIVE.name()).isEqualTo("ACTIVE");
		assertThat(OrganizationStatus.SUSPENDED.name()).isEqualTo("SUSPENDED");
		assertThat(OrganizationStatus.ARCHIVED.name()).isEqualTo("ARCHIVED");
	}
}
