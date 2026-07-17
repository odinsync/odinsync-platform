package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.domain.exception.InactiveTenantException;
import com.odinsync.identity.domain.exception.InactiveUserException;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;

class LoginUseCaseTest {

	@Test
	void logsInActiveUserAndReturnsAccessToken() {
		AuthenticatedUser authenticatedUser = activeUser(List.of("OWNER"));
		CapturingTokenGenerator tokenGenerator = new CapturingTokenGenerator();
		LoginUseCase useCase = new LoginUseCase(
				(email, password) -> authenticatedUser,
				tokenGenerator);

		LoginResult result = useCase.login(new LoginCommand(
				" Owner@Example.com ",
				"correct-password"));

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.tokenType()).isEqualTo("Bearer");
		assertThat(result.expiresIn()).isEqualTo(900);
		assertThat(result.tenantId()).isEqualTo(authenticatedUser.tenantId());
		assertThat(result.userId()).isEqualTo(authenticatedUser.userId());
		assertThat(result.roles()).containsExactly("OWNER");
	}

	@Test
	void rejectsInvalidCredentials() {
		LoginUseCase useCase = new LoginUseCase(
				(email, password) -> {
					throw new InvalidCredentialsException();
				},
				user -> new GeneratedAccessToken("unused", 900));

		assertThatThrownBy(() -> useCase.login(new LoginCommand(
				"owner@example.com",
				"wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void rejectsInactiveUser() {
		LoginUseCase useCase = new LoginUseCase(
				(email, password) -> new AuthenticatedUser(
						UUID.randomUUID(),
						UUID.randomUUID(),
						email,
						List.of("OWNER"),
						UserStatus.DISABLED,
						TenantStatus.ACTIVE),
				user -> new GeneratedAccessToken("unused", 900));

		assertThatThrownBy(() -> useCase.login(new LoginCommand(
				"owner@example.com",
				"correct-password")))
				.isInstanceOf(InactiveUserException.class);
	}

	@Test
	void rejectsInactiveTenant() {
		LoginUseCase useCase = new LoginUseCase(
				(email, password) -> new AuthenticatedUser(
						UUID.randomUUID(),
						UUID.randomUUID(),
						email,
						List.of("OWNER"),
						UserStatus.ACTIVE,
						TenantStatus.SUSPENDED),
				user -> new GeneratedAccessToken("unused", 900));

		assertThatThrownBy(() -> useCase.login(new LoginCommand(
				"owner@example.com",
				"correct-password")))
				.isInstanceOf(InactiveTenantException.class);
	}

	@Test
	void sendsAuthenticatedUserToTokenGenerator() {
		AuthenticatedUser authenticatedUser = activeUser(List.of("OWNER", "ADMIN"));
		CapturingTokenGenerator tokenGenerator = new CapturingTokenGenerator();
		LoginUseCase useCase = new LoginUseCase(
				(email, password) -> authenticatedUser,
				tokenGenerator);

		useCase.login(new LoginCommand("owner@example.com", "correct-password"));

		assertThat(tokenGenerator.capturedUser).isEqualTo(authenticatedUser);
		assertThat(tokenGenerator.capturedUser.tenantId()).isEqualTo(authenticatedUser.tenantId());
		assertThat(tokenGenerator.capturedUser.roles()).containsExactly("OWNER", "ADMIN");
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

	private static final class CapturingTokenGenerator implements AccessTokenGeneratorPort {
		private AuthenticatedUser capturedUser;

		@Override
		public GeneratedAccessToken generate(AuthenticatedUser user) {
			capturedUser = user;
			return new GeneratedAccessToken("access-token", 900);
		}
	}
}
