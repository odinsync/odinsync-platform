package com.odinsync.identity.infrastructure.security;

import java.util.Locale;

import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import com.odinsync.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.odinsync.identity.infrastructure.persistence.entity.TenantJpaEntity;
import com.odinsync.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.TenantJpaRepository;
import com.odinsync.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.odinsync.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OdinSyncUserDetailsService implements UserDetailsService {

	private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

	private final UserJpaRepository userRepository;
	private final TenantJpaRepository tenantRepository;
	private final UserRoleJpaRepository userRoleRepository;

	/**
	 * Loads a user, tenant, and roles for Spring Security username/password authentication.
	 */
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
		UserJpaEntity user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));
		TenantJpaEntity tenant = tenantRepository.findById(user.getTenantId())
				.orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));

		return new OdinSyncUserDetails(
				user.getId(),
				user.getTenantId(),
				user.getEmail(),
				user.getPasswordHash(),
				userRoleRepository.findRolesByUserId(user.getId())
						.stream()
						.map(RoleJpaEntity::getName)
						.toList(),
				UserStatus.valueOf(user.getStatus()),
				TenantStatus.valueOf(tenant.getStatus()));
	}
}
