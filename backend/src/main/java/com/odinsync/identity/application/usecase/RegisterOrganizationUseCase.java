package com.odinsync.identity.application.usecase;

import java.util.UUID;

import com.odinsync.identity.domain.exception.EmailAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.port.in.RegisterOrganizationPort;
import com.odinsync.identity.application.port.out.OrganizationRepositoryPort;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleRepositoryPort;
import com.odinsync.identity.domain.model.Organization;
import com.odinsync.identity.domain.model.Role;
import com.odinsync.identity.domain.model.Tenant;
import com.odinsync.identity.domain.model.SubscriptionPlan;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.User;
import com.odinsync.identity.domain.model.UserStatus;

@Service
class RegisterOrganizationUseCase implements RegisterOrganizationPort {
	private final TenantRepositoryPort tenantRepository;
	private final OrganizationRepositoryPort organizationRepository;
	private final UserRepositoryPort userRepository;
	private final UserRoleRepositoryPort roleRepository;
	private final PasswordEncoderPort passwordEncoder;

	RegisterOrganizationUseCase(
			TenantRepositoryPort tenantRepository,
			OrganizationRepositoryPort organizationRepository,
			UserRepositoryPort userRepository,
			UserRoleRepositoryPort roleRepository,
			PasswordEncoderPort passwordEncoder) {
		this.tenantRepository = tenantRepository;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
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
		Organization organization = organizationRepository.save(
				Organization.create(
						tenant.id(),
						command.organizationName(),
						command.legalName(),
						normalizedEmail
				)
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
		roleRepository.assignRole(
				ownerUser.id(), ownerRole.id()
		);

		return new RegisterOrganizationResult(
				tenant.id(),
				organization.id(),
				ownerUser.id(),
				"Organization registered successfully"

		);
	}
}
