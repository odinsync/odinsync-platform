package com.odinsync.identity.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class OdinSyncUserDetails implements UserDetails {

	private final UUID userId;
	private final UUID tenantId;
	private final String email;
	private final String passwordHash;
	private final List<String> roles;
	private final UserStatus userStatus;
	private final TenantStatus tenantStatus;

	/**
	 * Creates the Spring Security user-details snapshot used during credential authentication.
	 */
	OdinSyncUserDetails(
			UUID userId,
			UUID tenantId,
			String email,
			String passwordHash,
			List<String> roles,
			UserStatus userStatus,
			TenantStatus tenantStatus) {
		this.userId = userId;
		this.tenantId = tenantId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.roles = List.copyOf(roles);
		this.userStatus = userStatus;
		this.tenantStatus = tenantStatus;
	}

	/**
	 * Converts Spring Security user details into the application authentication model.
	 */
	AuthenticatedUser toAuthenticatedUser() {
		return new AuthenticatedUser(
				userId,
				tenantId,
				email,
				roles,
				userStatus,
				tenantStatus);
	}

	/**
	 * Exposes OdinSync role names as Spring Security ROLE_* authorities.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
	}

	/**
	 * Returns the stored password hash used by Spring Security credential checks.
	 */
	@Override
	public String getPassword() {
		return passwordHash;
	}

	/**
	 * Returns the normalized email address used as the Spring Security username.
	 */
	@Override
	public String getUsername() {
		return email;
	}

	/**
	 * Keeps account-expiration policy outside Spring's built-in flag for now.
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	/**
	 * Keeps account-lock policy outside Spring's built-in flag for now.
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	/**
	 * Keeps credential-expiration policy outside Spring's built-in flag for now.
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/**
	 * Keeps enabled-state enforcement in the application use cases.
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}
}
