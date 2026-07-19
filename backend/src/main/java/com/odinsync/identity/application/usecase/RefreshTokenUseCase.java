package com.odinsync.identity.application.usecase;

import com.odinsync.identity.application.command.RefreshTokenCommand;
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

	@Override
	@Transactional
	public RefreshTokenResult refresh(RefreshTokenCommand command) {
		RotatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(command.refreshToken());
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
}
