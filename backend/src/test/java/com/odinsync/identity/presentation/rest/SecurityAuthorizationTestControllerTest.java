package com.odinsync.identity.presentation.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odinsync.shared.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@WebMvcTest(SecurityAuthorizationTestController.class)
@ContextConfiguration(classes = {
		SecurityAuthorizationTestController.class,
		SecurityAuthorizationTestControllerTest.TestSecurityConfiguration.class
})
class SecurityAuthorizationTestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedEndpointWithJwtReturnsOk() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/authenticated").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Authenticated endpoint accessed"));
	}

	@Test
	void authenticatedEndpointWithoutJwtReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/authenticated"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void ownerEndpointAllowsOwner() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/owner").with(jwtWithRole("ROLE_OWNER")))
				.andExpect(status().isOk());
	}

	@Test
	void ownerEndpointRejectsAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/owner").with(jwtWithRole("ROLE_ADMIN")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void adminEndpointAllowsAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/admin").with(jwtWithRole("ROLE_ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void adminEndpointRejectsOwner() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/admin").with(jwtWithRole("ROLE_OWNER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void ownerOrAdminEndpointAllowsOwner() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/owner-or-admin").with(jwtWithRole("ROLE_OWNER")))
				.andExpect(status().isOk());
	}

	@Test
	void ownerOrAdminEndpointAllowsAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/owner-or-admin").with(jwtWithRole("ROLE_ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void ownerOrAdminEndpointRejectsMember() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/owner-or-admin").with(jwtWithRole("ROLE_MEMBER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void memberEndpointAllowsMember() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/member").with(jwtWithRole("ROLE_MEMBER")))
				.andExpect(status().isOk());
	}

	@Test
	void jwtWithoutRolesCanAccessAuthenticatedEndpointOnly() throws Exception {
		mockMvc.perform(get("/api/v1/security-test/authenticated").with(jwt().authorities()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/security-test/owner").with(jwt().authorities()))
				.andExpect(status().isForbidden());
	}

	private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(
			String role) {
		return jwt().authorities(new SimpleGrantedAuthority(role));
	}

	@TestConfiguration
	@EnableMethodSecurity
	static class TestSecurityConfiguration {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
			return http
					.csrf(AbstractHttpConfigurer::disable)
					.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
					.exceptionHandling(exceptionHandling -> exceptionHandling
							.authenticationEntryPoint((request, response, authException) -> {
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType(MediaType.APPLICATION_JSON_VALUE);
								objectMapper.writeValue(
										response.getOutputStream(),
										ApiErrorResponse.of("UNAUTHORIZED", "Authentication is required"));
							})
							.accessDeniedHandler((request, response, accessDeniedException) -> {
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								response.setContentType(MediaType.APPLICATION_JSON_VALUE);
								objectMapper.writeValue(
										response.getOutputStream(),
										ApiErrorResponse.of(
												"ACCESS_DENIED",
												"You do not have permission to access this resource"));
							}))
					.build();
		}
	}
}
