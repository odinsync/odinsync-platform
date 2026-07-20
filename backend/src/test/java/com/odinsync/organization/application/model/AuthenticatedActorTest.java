package com.odinsync.organization.application.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuthenticatedActorTest {

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	void createsImmutableActorSnapshot() {
		Set<String> roles = new HashSet<>();
		roles.add("OWNER");
		Set<String> permissions = new HashSet<>();
		permissions.add(OrganizationPermission.PROFILE_READ.value());

		AuthenticatedActor actor = new AuthenticatedActor(USER_ID, TENANT_ID, roles, permissions);

		roles.add("ADMIN");
		permissions.add(OrganizationPermission.SETTINGS_UPDATE.value());

		assertThat(actor.userId()).isEqualTo(USER_ID);
		assertThat(actor.tenantId()).isEqualTo(TENANT_ID);
		assertThat(actor.roles()).containsExactly("OWNER");
		assertThat(actor.permissions()).containsExactly(OrganizationPermission.PROFILE_READ.value());
		assertThatThrownBy(() -> actor.roles().add("ADMIN"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> actor.permissions().add(OrganizationPermission.SETTINGS_UPDATE.value()))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsNullArguments() {
		assertThatThrownBy(() -> new AuthenticatedActor(null, TENANT_ID, Set.of(), Set.of()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedActor(USER_ID, null, Set.of(), Set.of()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedActor(USER_ID, TENANT_ID, null, Set.of()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedActor(USER_ID, TENANT_ID, Set.of(), null))
				.isInstanceOf(NullPointerException.class);
	}
}
