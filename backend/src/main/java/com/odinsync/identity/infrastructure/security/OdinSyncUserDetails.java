package com.odinsync.identity.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.usecase.AuthenticatedUser;
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

	AuthenticatedUser toAuthenticatedUser() {
		return new AuthenticatedUser(
				userId,
				tenantId,
				email,
				roles,
				userStatus,
				tenantStatus);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
