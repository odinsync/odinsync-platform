package com.odinsync.identity.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OdinSyncJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final String ROLES_CLAIM = "roles";
	private static final String ROLE_PREFIX = "ROLE_";

	/**
	 * Converts a validated JWT into Spring Security authentication with role authorities.
	 */
	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = toAuthorities(jwt.getClaimAsStringList(ROLES_CLAIM));
		return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
	}

	/**
	 * Normalizes OdinSync role claims into Spring Security ROLE_* authorities.
	 */
	private static Collection<GrantedAuthority> toAuthorities(List<String> roles) {
		if (roles == null) {
			return List.of();
		}
		return roles.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(role -> !role.isBlank())
				.map(role -> role.toUpperCase(Locale.ROOT))
				.map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role)
				.distinct()
				.<GrantedAuthority>map(SimpleGrantedAuthority::new)
				.toList();
	}
}
