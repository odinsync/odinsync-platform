package com.odinsync.identity.presentation.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odinsync.shared.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentUserController.class)
@ContextConfiguration(classes = {
		CurrentUserController.class,
		CurrentUserControllerTest.TestSecurityConfiguration.class
})
class CurrentUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedRequestReturnsCurrentJwtIdentity() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();

		mockMvc.perform(get("/api/v1/users/me")
						.with(jwt().jwt(jwt -> jwt
								.subject(userId.toString())
								.claim("tenant_id", tenantId.toString())
								.claim("email", "owner@odinsync.com")
								.claim("roles", List.of("OWNER")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userId.toString()))
				.andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
				.andExpect(jsonPath("$.email").value("owner@odinsync.com"))
				.andExpect(jsonPath("$.roles[0]").value("OWNER"));
	}

	@Test
	void missingAuthenticationReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@TestConfiguration
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
							}))
					.build();
		}
	}
}
