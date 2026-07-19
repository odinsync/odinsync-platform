package com.odinsync.identity.presentation.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.port.in.LoginPort;
import com.odinsync.identity.application.model.LoginResult;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
import com.odinsync.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class LoginControllerSecurityTest {

	@Mock
	private LoginPort loginPort;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new LoginController(loginPort))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	void loginEndpointReturnsTokenWhenRequestIsValid() throws Exception {
		UUID tenantId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(loginPort.login(argThat(command ->
				"owner@example.com".equals(command.email())
						&& "correct-password".equals(command.password()))))
				.thenReturn(new LoginResult(
						"access-token",
						"Bearer",
						900,
						"refresh-token",
						Instant.parse("2026-08-18T00:00:00Z"),
						tenantId,
						userId,
						List.of("OWNER")));

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
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.refreshTokenExpiresAt").exists())
				.andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
				.andExpect(jsonPath("$.userId").value(userId.toString()))
				.andExpect(jsonPath("$.roles[0]").value("OWNER"));

		verify(loginPort).login(argThat(command ->
				"owner@example.com".equals(command.email())
						&& "correct-password".equals(command.password())));
	}

	@Test
	void invalidLoginRequestReturnsBadRequestWithoutCallingUseCase() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(loginPort);
	}

	@Test
	void invalidCredentialsReturnUnauthorized() throws Exception {
		when(loginPort.login(argThat(command ->
				"invalid@example.com".equals(command.email())
						&& "wrong-password".equals(command.password()))))
				.thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invalid@example.com",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		verify(loginPort).login(argThat(command ->
				"invalid@example.com".equals(command.email())
						&& "wrong-password".equals(command.password())));
	}
}
