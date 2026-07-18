package com.odinsync.identity.presentation.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.infrastructure.security.OdinSyncJwtAuthenticationConverter;
import com.odinsync.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
		classes = ProtectedApiJwtAuthenticationIntegrationTest.TestApplication.class,
		properties = "odinsync.security.jwt.generate-development-keys=true")
@AutoConfigureMockMvc
class ProtectedApiJwtAuthenticationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void validJwtAccessesProtectedCurrentUserEndpoint() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		String token = accessToken(userId, tenantId, "odinsync-platform", Instant.now().plusSeconds(900), List.of("OWNER"));

		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userId.toString()))
				.andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
				.andExpect(jsonPath("$.email").value("owner@odinsync.com"))
				.andExpect(jsonPath("$.roles[0]").value("OWNER"));
	}

	@Test
	void missingJwtReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void modifiedJwtReturnsUnauthorized() throws Exception {
		String token = accessToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"odinsync-platform",
				Instant.now().plusSeconds(900),
				List.of("OWNER"));

		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + modify(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void expiredJwtReturnsUnauthorized() throws Exception {
		String token = accessToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"odinsync-platform",
				Instant.now().minusSeconds(3600),
				List.of("OWNER"));

		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void incorrectIssuerReturnsUnauthorized() throws Exception {
		String token = accessToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"other-issuer",
				Instant.now().plusSeconds(900),
				List.of("OWNER"));

		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void validJwtPopulatesJwtAuthenticationTokenAndRoleAuthorities() throws Exception {
		String token = accessToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"odinsync-platform",
				Instant.now().plusSeconds(900),
				List.of("OWNER"));

		mockMvc.perform(get("/test/authentication")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andExpect(jsonPath("$.authenticationType").value(JwtAuthenticationToken.class.getName()))
				.andExpect(jsonPath("$.authorities[0]").value("ROLE_OWNER"));
	}

	@Test
	void ownerTokenCanUseOwnerEndpointsButNotAdminEndpoint() throws Exception {
		String token = accessToken(List.of("OWNER"));

		mockMvc.perform(get("/api/v1/security-test/authenticated")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/security-test/owner")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/security-test/admin")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/v1/security-test/owner-or-admin")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void adminTokenCanUseAdminAndOwnerOrAdminEndpointsButNotOwnerEndpoint() throws Exception {
		String token = accessToken(List.of("ADMIN"));

		mockMvc.perform(get("/api/v1/security-test/admin")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/security-test/owner")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/v1/security-test/owner-or-admin")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void memberTokenCanUseMemberEndpointButNotOwnerOrAdminEndpoints() throws Exception {
		String token = accessToken(List.of("MEMBER"));

		mockMvc.perform(get("/api/v1/security-test/member")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/security-test/admin")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/v1/security-test/owner")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void protectedTestEndpointWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/authenticated"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private String accessToken(List<String> roles) {
		return accessToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"odinsync-platform",
				Instant.now().plusSeconds(900),
				roles);
	}

	private String accessToken(UUID userId, UUID tenantId, String issuer, Instant expiresAt, List<String> roles) {
		Instant issuedAt = expiresAt.isBefore(Instant.now())
				? expiresAt.minusSeconds(900)
				: Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(userId.toString())
				.id(UUID.randomUUID().toString())
				.claim("tenant_id", tenantId.toString())
				.claim("email", "owner@odinsync.com")
				.claim("roles", roles)
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
						JwsHeader.with(SignatureAlgorithm.RS256).build(),
						claims))
				.getTokenValue();
	}

	private static String modify(String token) {
		char replacement = token.charAt(token.length() - 1) == 'a' ? 'b' : 'a';
		return token.substring(0, token.length() - 1) + replacement;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = {
			DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class,
			FlywayAutoConfiguration.class
	})
	@ComponentScan(basePackages = "com.odinsync.shared.security")
	@Import({
			CurrentUserController.class,
			SecurityAuthorizationTestController.class,
			OdinSyncJwtAuthenticationConverter.class,
			TestAuthenticationController.class
	})
	static class TestApplication {

		@Bean
		UserDetailsService userDetailsService() {
			return username -> User.withUsername(username)
					.password("{noop}password")
					.roles("TEST")
					.build();
		}
	}

	@RestController
	static class TestAuthenticationController {

		@GetMapping("/test/authentication")
		AuthenticationResponse authentication(Authentication authentication) {
			assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
			return new AuthenticationResponse(
					authentication.isAuthenticated(),
					authentication.getClass().getName(),
					authentication.getAuthorities().stream()
							.map(GrantedAuthority::getAuthority)
							.toList(),
					authentication.getName());
		}
	}

	record AuthenticationResponse(
			boolean authenticated,
			String authenticationType,
			Collection<String> authorities,
			String name) {
	}
}
