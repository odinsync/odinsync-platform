package com.odinsync.identity.application.usecase;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;
import com.odinsync.identity.application.model.IssuedRefreshToken;
import com.odinsync.identity.application.model.LoginResult;
import com.odinsync.identity.application.port.in.LoginPort;
import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.application.port.out.CredentialsAuthenticatorPort;
import com.odinsync.identity.domain.exception.InactiveTenantException;
import com.odinsync.identity.domain.exception.InactiveUserException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LoginUseCase implements LoginPort {

	private static final String TOKEN_TYPE = "Bearer";

	private final CredentialsAuthenticatorPort credentialsAuthenticator;
	private final AccessTokenGeneratorPort accessTokenGenerator;
	private final RefreshTokenService refreshTokenService;

	LoginUseCase(
			CredentialsAuthenticatorPort credentialsAuthenticator,
			AccessTokenGeneratorPort accessTokenGenerator,
			RefreshTokenService refreshTokenService) {
		this.credentialsAuthenticator = credentialsAuthenticator;
		this.accessTokenGenerator = accessTokenGenerator;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	@Transactional
	public LoginResult login(LoginCommand command) {
		AuthenticatedUser user = credentialsAuthenticator.authenticate(
				command.normalizedEmail(),
				command.password());

		if (user.userStatus() != UserStatus.ACTIVE) {
			throw new InactiveUserException();
		}
		if (user.tenantStatus() != TenantStatus.ACTIVE) {
			throw new InactiveTenantException();
		}

		GeneratedAccessToken accessToken = accessTokenGenerator.generate(user);
		IssuedRefreshToken refreshToken = refreshTokenService.issue(
				user.userId(),
				user.tenantId(),
				java.util.UUID.randomUUID(),
				command.metadata());
		return new LoginResult(
				accessToken.value(),
				TOKEN_TYPE,
				accessToken.expiresIn(),
				refreshToken.rawToken(),
				refreshToken.expiresAt(),
				user.tenantId(),
				user.userId(),
				user.roles());
	}
}
