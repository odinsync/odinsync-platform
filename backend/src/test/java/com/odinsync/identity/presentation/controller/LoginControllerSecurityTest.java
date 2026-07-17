package com.odinsync.identity.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.port.in.LoginPort;
import com.odinsync.identity.application.usecase.LoginResult;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
		classes = LoginControllerSecurityTest.TestApplication.class,
		properties = {
				"odinsync.security.jwt.generate-development-keys=true"
		})
@AutoConfigureMockMvc
class LoginControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void loginEndpointIsPublic() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "owner@example.com",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.roles[0]").value("OWNER"));
	}

	@Test
	void invalidLoginRequestReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void invalidCredentialsReturnUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invalid@example.com",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/test/protected"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void validJwtCanAccessProtectedEndpoint() throws Exception {
		String token = createAccessToken();

		mockMvc.perform(get("/test/protected")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	private String createAccessToken() {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("odinsync-platform")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900))
				.subject(UUID.randomUUID().toString())
				.id(UUID.randomUUID().toString())
				.claim("tenant_id", UUID.randomUUID().toString())
				.claim("email", "owner@example.com")
				.claim("roles", List.of("OWNER"))
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
						JwsHeader.with(SignatureAlgorithm.RS256).build(),
						claims))
				.getTokenValue();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = {
			DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class,
			FlywayAutoConfiguration.class
	})
	@ComponentScan(basePackages = {
			"com.odinsync.shared.security",
			"com.odinsync.shared.exception"
	})
	@Import({
			LoginController.class,
			ProtectedTestController.class
	})
	static class TestApplication {

		@Bean
		LoginPort loginPort() {
			return command -> {
				if ("invalid@example.com".equals(command.normalizedEmail())) {
					throw new InvalidCredentialsException();
				}
				return new LoginResult(
						"access-token",
						"Bearer",
						900,
						UUID.randomUUID(),
						UUID.randomUUID(),
						List.of("OWNER"));
			};
		}

		@Bean
		UserDetailsService userDetailsService() {
			return username -> User.withUsername(username)
					.password("{noop}password")
					.roles("TEST")
					.build();
		}
	}

	@RestController
	static class ProtectedTestController {

		@GetMapping("/test/protected")
		TestResponse protectedEndpoint() {
			return new TestResponse("ok");
		}
	}

	record TestResponse(String status) {
	}
}
