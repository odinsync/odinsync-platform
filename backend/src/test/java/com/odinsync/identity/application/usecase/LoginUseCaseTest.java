package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;
import com.odinsync.identity.application.model.IssuedRefreshToken;
import com.odinsync.identity.application.model.LoginResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.application.port.out.CredentialsAuthenticatorPort;
import com.odinsync.identity.domain.exception.InactiveTenantException;
import com.odinsync.identity.domain.exception.InactiveUserException;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

	@Mock
	private CredentialsAuthenticatorPort credentialsAuthenticator;

	@Mock
	private AccessTokenGeneratorPort accessTokenGenerator;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private LoginUseCase loginUseCase;

	@Test
	void authenticatesWithNormalizedEmailAndReturnsAccessToken() {
		AuthenticatedUser authenticatedUser = activeUser(List.of("OWNER"));
		GeneratedAccessToken generatedToken = new GeneratedAccessToken("signed-jwt", 900);
		IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"refresh-token",
				Instant.parse("2026-08-18T00:00:00Z"));
		when(credentialsAuthenticator.authenticate("owner@example.com", "Password@123"))
				.thenReturn(authenticatedUser);
		when(accessTokenGenerator.generate(authenticatedUser))
				.thenReturn(generatedToken);
		when(refreshTokenService.issue(authenticatedUser.userId(), authenticatedUser.tenantId()))
				.thenReturn(issuedRefreshToken);

		LoginResult result = loginUseCase.login(new LoginCommand(
				" Owner@Example.com ",
				"Password@123"));

		assertThat(result.accessToken()).isEqualTo("signed-jwt");
		assertThat(result.tokenType()).isEqualTo("Bearer");
		assertThat(result.expiresIn()).isEqualTo(900);
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.refreshTokenExpiresAt()).isEqualTo(issuedRefreshToken.expiresAt());
		assertThat(result.tenantId()).isEqualTo(authenticatedUser.tenantId());
		assertThat(result.userId()).isEqualTo(authenticatedUser.userId());
		assertThat(result.roles()).containsExactly("OWNER");
		verify(credentialsAuthenticator).authenticate("owner@example.com", "Password@123");
		verify(accessTokenGenerator).generate(authenticatedUser);
	}

	@Test
	void passesAuthenticatedUserDataToTokenGenerator() {
		AuthenticatedUser authenticatedUser = activeUser(List.of("OWNER", "ADMIN"));
		when(credentialsAuthenticator.authenticate("owner@example.com", "correct-password"))
				.thenReturn(authenticatedUser);
		when(accessTokenGenerator.generate(authenticatedUser))
				.thenReturn(new GeneratedAccessToken("access-token", 900));
		when(refreshTokenService.issue(authenticatedUser.userId(), authenticatedUser.tenantId()))
				.thenReturn(new IssuedRefreshToken(
						UUID.randomUUID(),
						UUID.randomUUID(),
						"refresh-token",
						Instant.parse("2026-08-18T00:00:00Z")));

		loginUseCase.login(new LoginCommand("owner@example.com", "correct-password"));

		verify(accessTokenGenerator).generate(authenticatedUser);
	}

	@Test
	void propagatesInvalidCredentialsAndDoesNotGenerateToken() {
		when(credentialsAuthenticator.authenticate("owner@example.com", "wrong-password"))
				.thenThrow(new InvalidCredentialsException());

		assertThatThrownBy(() -> loginUseCase.login(new LoginCommand(
				"owner@example.com",
				"wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);
		verifyNoInteractions(accessTokenGenerator);
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void rejectsInactiveUserAndDoesNotGenerateToken() {
		AuthenticatedUser inactiveUser = new AuthenticatedUser(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"owner@example.com",
				List.of("OWNER"),
				UserStatus.DISABLED,
				TenantStatus.ACTIVE);
		when(credentialsAuthenticator.authenticate("owner@example.com", "correct-password"))
				.thenReturn(inactiveUser);

		assertThatThrownBy(() -> loginUseCase.login(new LoginCommand(
				"owner@example.com",
				"correct-password")))
				.isInstanceOf(InactiveUserException.class);
		verifyNoInteractions(accessTokenGenerator);
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void rejectsInactiveTenantAndDoesNotGenerateToken() {
		AuthenticatedUser inactiveTenantUser = new AuthenticatedUser(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"owner@example.com",
				List.of("OWNER"),
				UserStatus.ACTIVE,
				TenantStatus.SUSPENDED);
		when(credentialsAuthenticator.authenticate("owner@example.com", "correct-password"))
				.thenReturn(inactiveTenantUser);

		assertThatThrownBy(() -> loginUseCase.login(new LoginCommand(
				"owner@example.com",
				"correct-password")))
				.isInstanceOf(InactiveTenantException.class);
		verifyNoInteractions(accessTokenGenerator);
		verifyNoInteractions(refreshTokenService);
	}

	private static AuthenticatedUser activeUser(List<String> roles) {
		return new AuthenticatedUser(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"owner@example.com",
				roles,
				UserStatus.ACTIVE,
				TenantStatus.ACTIVE);
	}
}
