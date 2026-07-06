package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.port.out.OrganizationRepositoryPort;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleRepositoryPort;
import com.odinsync.identity.domain.exception.EmailAlreadyExistsException;
import com.odinsync.identity.domain.model.Organization;
import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.domain.model.RoleName;
import com.odinsync.identity.domain.model.Tenant;
import com.odinsync.identity.domain.model.SubscriptionPlan;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.User;
import com.odinsync.identity.domain.model.UserStatus;

class RegisterOrganizationUseCaseTest {

	@Test
	void registersOrganizationAndOwner() {
		InMemoryTenantRepository tenants = new InMemoryTenantRepository();
		InMemoryOrganizationRepository organizations = new InMemoryOrganizationRepository();
		InMemoryUserRepository users = new InMemoryUserRepository();
		InMemoryUserRoleRepository userRoles = new InMemoryUserRoleRepository();

		RegisterOrganizationUseCase service = new RegisterOrganizationUseCase(
				tenants,
				organizations,
				users,
				userRoles,
				rawPassword -> "hashed-" + rawPassword);

		RegisterOrganizationResult result = service.register(new RegisterOrganizationCommand(
				"Odin Retail",
				"Odin Retail Private Limited",
				"Ada Lovelace",
				"Owner@Example.com",
				"strong-password"));

		assertThat(result.tenantId()).isNotNull();
		assertThat(result.organizationId()).isNotNull();
		assertThat(result.userId()).isNotNull();
		assertThat(result.message()).isEqualTo("Organization registered successfully");

		Tenant tenant = tenants.savedTenant;
		assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
		assertThat(tenant.plan()).isEqualTo(SubscriptionPlan.FREE);

		Organization organization = organizations.savedOrganization;
		assertThat(organization.tenantId()).isEqualTo(tenant.id());
		assertThat(organization.name()).isEqualTo("Odin Retail");

		User owner = users.savedUser;
		assertThat(owner.tenantId()).isEqualTo(tenant.id());
		assertThat(owner.email()).isEqualTo("owner@example.com");
		assertThat(owner.passwordHash()).isEqualTo("hashed-strong-password");
		assertThat(owner.status()).isEqualTo(UserStatus.ACTIVE);

		Role ownerRole = userRoles.savedRole;
		assertThat(ownerRole.tenantId()).isEqualTo(tenant.id());
		assertThat(ownerRole.name()).isEqualTo(RoleName.OWNER);
		assertThat(userRoles.assignedUserId).isEqualTo(owner.id());
		assertThat(userRoles.assignedRoleId).isEqualTo(ownerRole.id());
	}

	@Test
	void rejectsDuplicateEmail() {
		InMemoryUserRepository users = new InMemoryUserRepository();
		users.existingEmails.put("owner@example.com", true);

		RegisterOrganizationUseCase service = new RegisterOrganizationUseCase(
				new InMemoryTenantRepository(),
				new InMemoryOrganizationRepository(),
				users,
				new InMemoryUserRoleRepository(),
				new NoOpPasswordEncoderPort());

		RegisterOrganizationCommand command = new RegisterOrganizationCommand(
				"Odin Retail",
				"Odin Retail Private Limited",
				"Ada Lovelace",
				"owner@example.com",
				"strong-password");

		assertThatThrownBy(() -> service.register(command))
				.isInstanceOf(EmailAlreadyExistsException.class)
				.hasMessageContaining("owner@example.com");
	}

	private static final class InMemoryTenantRepository implements TenantRepositoryPort {
		private Tenant savedTenant;

		@Override
		public Tenant save(Tenant tenant) {
			savedTenant = tenant;
			return tenant;
		}
	}

	private static final class InMemoryOrganizationRepository implements OrganizationRepositoryPort {
		private Organization savedOrganization;

		@Override
		public Organization save(Organization organization) {
			savedOrganization = organization;
			return organization;
		}
	}

	private static final class InMemoryUserRepository implements UserRepositoryPort {
		private final Map<String, Boolean> existingEmails = new HashMap<>();
		private User savedUser;

		@Override
		public boolean existsByEmail(String email) {
			return existingEmails.containsKey(email);
		}

		@Override
		public User save(User user) {
			savedUser = user;
			existingEmails.put(user.email(), true);
			return user;
		}
	}

	private static final class InMemoryUserRoleRepository implements UserRoleRepositoryPort {
		private Role savedRole;
		private UUID assignedUserId;
		private UUID assignedRoleId;

		@Override
		public Role save(Role role) {
			savedRole = role;
			return role;
		}

		@Override
		public void assignRole(UUID userId, UUID roleId) {
			assignedUserId = userId;
			assignedRoleId = roleId;
		}
	}

	private static final class NoOpPasswordEncoderPort implements PasswordEncoderPort {
		@Override
		public String encode(String rawPassword) {
			return rawPassword;
		}
	}
}
