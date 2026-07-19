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

import com.odinsync.identity.application.port.in.RefreshTokenPort;
import com.odinsync.identity.application.model.RefreshTokenResult;
import com.odinsync.identity.domain.exception.InvalidRefreshTokenException;
import com.odinsync.identity.domain.exception.RefreshTokenReuseDetectedException;
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
class RefreshTokenControllerTest {

	@Mock
	private RefreshTokenPort refreshTokenPort;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new RefreshTokenController(refreshTokenPort))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	void refreshEndpointReturnsRotatedTokenPair() throws Exception {
		UUID tenantId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(refreshTokenPort.refresh(argThat(command -> "old-refresh-token".equals(command.refreshToken()))))
				.thenReturn(new RefreshTokenResult(
						"new-access-token",
						"Bearer",
						900,
						"new-refresh-token",
						Instant.parse("2026-08-18T00:00:00Z"),
						tenantId,
						userId,
						List.of("OWNER")));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "old-refresh-token"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("new-access-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
				.andExpect(jsonPath("$.refreshTokenExpiresAt").exists())
				.andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
				.andExpect(jsonPath("$.userId").value(userId.toString()))
				.andExpect(jsonPath("$.roles[0]").value("OWNER"));

		verify(refreshTokenPort).refresh(argThat(command -> "old-refresh-token".equals(command.refreshToken())));
	}

	@Test
	void invalidRefreshRequestReturnsBadRequestWithoutCallingUseCase() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(refreshTokenPort);
	}

	@Test
	void invalidRefreshTokenReturnsUnauthorized() throws Exception {
		when(refreshTokenPort.refresh(argThat(command -> "invalid-token".equals(command.refreshToken()))))
				.thenThrow(new InvalidRefreshTokenException());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "invalid-token"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void reusedRefreshTokenReturnsUnauthorized() throws Exception {
		when(refreshTokenPort.refresh(argThat(command -> "reused-token".equals(command.refreshToken()))))
				.thenThrow(new RefreshTokenReuseDetectedException());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "reused-token"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE_DETECTED"));
	}
}
