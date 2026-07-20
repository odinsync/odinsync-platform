package com.odinsync.organization.application.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationPermissionTest {

	@Test
	void centralizesDocumentedOrganizationPermissionNames() {
		assertThat(OrganizationPermission.PROFILE_READ.value()).isEqualTo("organization:read");
		assertThat(OrganizationPermission.PROFILE_UPDATE.value()).isEqualTo("organization:update");
		assertThat(OrganizationPermission.SETTINGS_READ.value()).isEqualTo("organization:settings:read");
		assertThat(OrganizationPermission.SETTINGS_UPDATE.value()).isEqualTo("organization:settings:update");
	}
}
