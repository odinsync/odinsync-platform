package com.odinsync.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class OdinSyncJwtAuthenticationConverterTest {

	private final OdinSyncJwtAuthenticationConverter converter = new OdinSyncJwtAuthenticationConverter();

	@Test
	void convertsRoleClaimsToSpringAuthorities() {
		JwtAuthenticationToken authentication = convert(jwtWithRoles(List.of("OWNER", "ADMIN")));

		assertThat(authentication.getAuthorities())
				.containsExactlyInAnyOrder(
						new SimpleGrantedAuthority("ROLE_OWNER"),
						new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	@Test
	void missingRolesProducesEmptyAuthorities() {
		JwtAuthenticationToken authentication = convert(jwtWithoutRoles());

		assertThat(authentication.getAuthorities()).isEmpty();
	}

	@Test
	void existingRolePrefixIsNotDuplicated() {
		JwtAuthenticationToken authentication = convert(jwtWithRoles(List.of("ROLE_OWNER")));

		assertThat(authentication.getAuthorities())
				.containsExactly(new SimpleGrantedAuthority("ROLE_OWNER"));
	}

	@Test
	void blankDuplicateAndNullRolesAreIgnored() {
		List<String> roles = new ArrayList<>();
		roles.add("OWNER");
		roles.add("OWNER");
		roles.add(" ");
		roles.add(null);
		JwtAuthenticationToken authentication = convert(jwtWithRoles(roles));

		assertThat(authentication.getAuthorities())
				.containsExactly(new SimpleGrantedAuthority("ROLE_OWNER"));
	}

	@Test
	void subjectBecomesAuthenticationName() {
		Jwt jwt = jwtWithRoles(List.of("OWNER"));

		JwtAuthenticationToken authentication = convert(jwt);

		assertThat(authentication.getName()).isEqualTo(jwt.getSubject());
	}

	private JwtAuthenticationToken convert(Jwt jwt) {
		return (JwtAuthenticationToken) converter.convert(jwt);
	}

	private static Jwt jwtWithRoles(List<String> roles) {
		return jwtBuilder()
				.claim("roles", roles)
				.build();
	}

	private static Jwt jwtWithoutRoles() {
		return jwtBuilder().build();
	}

	private static Jwt.Builder jwtBuilder() {
		Instant issuedAt = Instant.now();
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject(userId.toString())
				.claim("tenant_id", tenantId.toString())
				.claim("email", "owner@odinsync.com")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900));
	}
}
