package com.odinsync.identity.application.usecase;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.command.LogoutCommand;
import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.model.ActiveSession;
import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;
import com.odinsync.identity.application.model.RefreshTokenResult;
import com.odinsync.identity.application.model.RotatedRefreshToken;
import com.odinsync.identity.application.port.in.RefreshTokenPort;
import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.application.port.out.AuthenticatedUserLookupPort;
import com.odinsync.identity.domain.exception.InactiveTenantException;
import com.odinsync.identity.domain.exception.InactiveUserException;
import com.odinsync.identity.domain.exception.InvalidRefreshTokenException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RefreshTokenUseCase implements RefreshTokenPort {

	private static final String TOKEN_TYPE = "Bearer";

	private final RefreshTokenService refreshTokenService;
	private final AuthenticatedUserLookupPort authenticatedUserLookup;
	private final AccessTokenGeneratorPort accessTokenGenerator;

	/**
	 * Rotates the refresh token and issues a new access token using current user state.
	 */
	@Override
	@Transactional
	public RefreshTokenResult refresh(RefreshTokenCommand command) {
		RotatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(command.refreshToken(), command.metadata());
		AuthenticatedUser user = authenticatedUserLookup.findById(rotatedRefreshToken.userId())
				.orElseThrow(InvalidRefreshTokenException::new);

		if (user.userStatus() != UserStatus.ACTIVE) {
			throw new InactiveUserException();
		}
		if (user.tenantStatus() != TenantStatus.ACTIVE) {
			throw new InactiveTenantException();
		}

		GeneratedAccessToken accessToken = accessTokenGenerator.generate(user);
		return new RefreshTokenResult(
				accessToken.value(),
				TOKEN_TYPE,
				accessToken.expiresIn(),
				rotatedRefreshToken.rawRefreshToken(),
				rotatedRefreshToken.expiresAt(),
				user.tenantId(),
				user.userId(),
				user.roles());
	}

	/**
	 * Logs out the session identified by the submitted refresh token.
	 */
	@Override
	@Transactional
	public void logout(LogoutCommand command) {
		refreshTokenService.logout(command.refreshToken());
	}

	/**
	 * Revokes all refresh-token sessions for a user within the current tenant.
	 */
	@Override
	@Transactional
	public void logoutAll(UUID userId, UUID tenantId) {
		refreshTokenService.logoutAll(userId, tenantId);
	}

	/**
	 * Retrieves active sessions for display without exposing credentials.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ActiveSession> activeSessions(UUID userId, UUID tenantId) {
		return refreshTokenService.getActiveSessions(userId, tenantId);
	}

	/**
	 * Revokes one session after tenant and user ownership are supplied by the caller.
	 */
	@Override
	@Transactional
	public void revokeSession(UUID sessionId, UUID userId, UUID tenantId) {
		refreshTokenService.revokeSession(sessionId, userId, tenantId);
	}
}
