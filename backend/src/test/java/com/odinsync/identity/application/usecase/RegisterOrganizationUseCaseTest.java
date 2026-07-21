package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.odinsync.identity.application.model.RegisterOrganizationResult;
import com.odinsync.organization.application.command.ProvisionOrganizationCommand;
import com.odinsync.organization.application.port.in.ProvisionOrganizationUseCase;
import com.odinsync.organization.application.result.ProvisionedOrganizationResult;
import org.junit.jupiter.api.Test;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleAssignmentPort;
import com.odinsync.identity.domain.exception.EmailAlreadyExistsException;
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
		InMemoryOrganizationProvisioning organizations = new InMemoryOrganizationProvisioning();
		InMemoryUserRepository users = new InMemoryUserRepository();
		InMemoryRoleRepository roles = new InMemoryRoleRepository();
		InMemoryUserRoleAssignment userRoleAssignments = new InMemoryUserRoleAssignment();

		RegisterOrganizationUseCase service = new RegisterOrganizationUseCase(
				tenants,
				organizations,
				users,
				roles,
				userRoleAssignments,
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

		User owner = users.savedUser;
		assertThat(owner.tenantId()).isEqualTo(tenant.id());
		assertThat(owner.email()).isEqualTo("owner@example.com");
		assertThat(owner.passwordHash()).isEqualTo("hashed-strong-password");
		assertThat(owner.status()).isEqualTo(UserStatus.ACTIVE);

		ProvisionOrganizationCommand provisioning = organizations.savedCommand;
		assertThat(provisioning.tenantId()).isEqualTo(tenant.id());
		assertThat(provisioning.ownerUserId()).isEqualTo(owner.id());
		assertThat(provisioning.organizationName()).isEqualTo("Odin Retail");
		assertThat(provisioning.legalName()).isEqualTo("Odin Retail Private Limited");
		assertThat(provisioning.contactEmail()).isEqualTo("owner@example.com");
		assertThat(result.organizationId()).isEqualTo(InMemoryOrganizationProvisioning.ORGANIZATION_ID);

		Role ownerRole = roles.savedRole;
		assertThat(ownerRole.tenantId()).isEqualTo(tenant.id());
		assertThat(ownerRole.name()).isEqualTo(RoleName.OWNER);
		assertThat(userRoleAssignments.assignedUserId).isEqualTo(owner.id());
		assertThat(userRoleAssignments.assignedRoleId).isEqualTo(ownerRole.id());
	}

	@Test
	void rejectsDuplicateEmail() {
		InMemoryUserRepository users = new InMemoryUserRepository();
		users.existingEmails.put("owner@example.com", true);

			RegisterOrganizationUseCase service = new RegisterOrganizationUseCase(
					new InMemoryTenantRepository(),
				new InMemoryOrganizationProvisioning(),
				users,
				new InMemoryRoleRepository(),
				new InMemoryUserRoleAssignment(),
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

	@Test
	void propagatesProvisioningFailureAfterIdentityBootstrapWork() {
		InMemoryTenantRepository tenants = new InMemoryTenantRepository();
		FailingOrganizationProvisioning organizations = new FailingOrganizationProvisioning();
		InMemoryUserRepository users = new InMemoryUserRepository();
		InMemoryRoleRepository roles = new InMemoryRoleRepository();
		InMemoryUserRoleAssignment userRoleAssignments = new InMemoryUserRoleAssignment();
		RuntimeException failure = organizations.failure;

		RegisterOrganizationUseCase service = new RegisterOrganizationUseCase(
				tenants,
				organizations,
				users,
				roles,
				userRoleAssignments,
				new NoOpPasswordEncoderPort());

		assertThatThrownBy(() -> service.register(new RegisterOrganizationCommand(
				"Odin Retail",
				"Odin Retail Private Limited",
				"Ada Lovelace",
				"owner@example.com",
				"strong-password")))
				.isSameAs(failure);

		assertThat(tenants.savedTenant).isNotNull();
		assertThat(users.savedUser).isNotNull();
		assertThat(roles.savedRole).isNotNull();
		assertThat(userRoleAssignments.assignedRoleId).isNotNull();
	}

	private static final class InMemoryTenantRepository implements TenantRepositoryPort {
		private Tenant savedTenant;

		@Override
		public Tenant save(Tenant tenant) {
			savedTenant = tenant;
			return tenant;
		}
	}

	private static final class InMemoryOrganizationProvisioning implements ProvisionOrganizationUseCase {
		private static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
		private ProvisionOrganizationCommand savedCommand;

		@Override
		public ProvisionedOrganizationResult provision(ProvisionOrganizationCommand command) {
			savedCommand = command;
			return new ProvisionedOrganizationResult(ORGANIZATION_ID, command.tenantId());
		}
	}

	private static final class FailingOrganizationProvisioning implements ProvisionOrganizationUseCase {
		private final RuntimeException failure = new RuntimeException("organization provisioning failed");

		@Override
		public ProvisionedOrganizationResult provision(ProvisionOrganizationCommand command) {
			throw failure;
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

	private static final class InMemoryRoleRepository implements RoleRepositoryPort {
		private Role savedRole;

		@Override
		public Role save(Role role) {
			savedRole = role;
			return role;
		}
	}

	private static final class InMemoryUserRoleAssignment implements UserRoleAssignmentPort {
		private UUID assignedUserId;
		private UUID assignedRoleId;

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
