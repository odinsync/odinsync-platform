package com.odinsync.identity.application.usecase;

import com.odinsync.identity.application.model.RegisterOrganizationResult;
import com.odinsync.identity.domain.exception.EmailAlreadyExistsException;
import com.odinsync.organization.application.command.ProvisionOrganizationCommand;
import com.odinsync.organization.application.port.in.ProvisionOrganizationUseCase;
import com.odinsync.organization.application.result.ProvisionedOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.port.in.RegisterOrganizationPort;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleAssignmentPort;
import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.domain.model.Tenant;
import com.odinsync.identity.domain.model.User;

@Service
class RegisterOrganizationUseCase implements RegisterOrganizationPort {
	private final TenantRepositoryPort tenantRepository;
	private final ProvisionOrganizationUseCase provisionOrganizationUseCase;
	private final UserRepositoryPort userRepository;
	private final RoleRepositoryPort roleRepository;
	private final UserRoleAssignmentPort userRoleAssignment;
	private final PasswordEncoderPort passwordEncoder;

	RegisterOrganizationUseCase(
			TenantRepositoryPort tenantRepository,
			ProvisionOrganizationUseCase provisionOrganizationUseCase,
			UserRepositoryPort userRepository,
			RoleRepositoryPort roleRepository,
			UserRoleAssignmentPort userRoleAssignment,
			PasswordEncoderPort passwordEncoder) {
		this.tenantRepository = tenantRepository;
		this.provisionOrganizationUseCase = provisionOrganizationUseCase;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleAssignment = userRoleAssignment;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public RegisterOrganizationResult register(RegisterOrganizationCommand command) {
		String normalizedEmail = command.normalEmail();
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyExistsException(normalizedEmail);
		}

		Tenant tenant = tenantRepository.save(
				Tenant.createFreeTenant(command.organizationName())
		);

		String passwordHash = passwordEncoder.encode(command.password());
		User ownerUser = userRepository.save(
				User.createOwner(
						tenant.id(),
						command.ownerName(),
						normalizedEmail,
						passwordHash
				)
		);

		Role ownerRole = roleRepository.save(Role.ownerRole(tenant.id()));
		userRoleAssignment.assignRole(
				ownerUser.id(), ownerRole.id()
		);
		ProvisionedOrganizationResult organization = provisionOrganizationUseCase.provision(
				new ProvisionOrganizationCommand(
						tenant.id(),
						ownerUser.id(),
						command.organizationName(),
						command.legalName(),
						normalizedEmail
				)
		);

		return new RegisterOrganizationResult(
				tenant.id(),
				organization.organizationId(),
				ownerUser.id(),
				"Organization registered successfully"

		);
	}
}
