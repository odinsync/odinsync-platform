package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;
import com.odinsync.identity.application.model.RefreshTokenResult;
import com.odinsync.identity.application.model.RotatedRefreshToken;
import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.application.port.out.AuthenticatedUserLookupPort;
import com.odinsync.identity.domain.exception.InvalidRefreshTokenException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private AuthenticatedUserLookupPort authenticatedUserLookup;

	@Mock
	private AccessTokenGeneratorPort accessTokenGenerator;

	@InjectMocks
	private RefreshTokenUseCase refreshTokenUseCase;

	@Test
	void rotatesRefreshTokenAndReturnsNewTokenPair() {
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		AuthenticatedUser user = new AuthenticatedUser(
				userId,
				tenantId,
				"owner@example.com",
				List.of("OWNER"),
				UserStatus.ACTIVE,
				TenantStatus.ACTIVE);
		when(refreshTokenService.rotate("old-refresh-token"))
				.thenReturn(new RotatedRefreshToken(
						userId,
						tenantId,
						"new-refresh-token",
						Instant.parse("2026-08-18T00:00:00Z")));
		when(authenticatedUserLookup.findById(userId)).thenReturn(Optional.of(user));
		when(accessTokenGenerator.generate(user)).thenReturn(new GeneratedAccessToken("new-access-token", 900));

		RefreshTokenResult result = refreshTokenUseCase.refresh(new RefreshTokenCommand("old-refresh-token"));

		assertThat(result.accessToken()).isEqualTo("new-access-token");
		assertThat(result.tokenType()).isEqualTo("Bearer");
		assertThat(result.expiresIn()).isEqualTo(900);
		assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
		assertThat(result.refreshTokenExpiresAt()).isEqualTo(Instant.parse("2026-08-18T00:00:00Z"));
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.tenantId()).isEqualTo(tenantId);
		assertThat(result.roles()).containsExactly("OWNER");
	}

	@Test
	void failsWhenRefreshTokenUserCannotBeLoaded() {
		UUID userId = UUID.randomUUID();
		when(refreshTokenService.rotate("refresh-token"))
				.thenReturn(new RotatedRefreshToken(
						userId,
						UUID.randomUUID(),
						"new-refresh-token",
						Instant.parse("2026-08-18T00:00:00Z")));
		when(authenticatedUserLookup.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenUseCase.refresh(new RefreshTokenCommand("refresh-token")))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}
}
