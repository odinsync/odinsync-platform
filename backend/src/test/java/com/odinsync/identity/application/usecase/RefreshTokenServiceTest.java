package com.odinsync.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.model.IssuedRefreshToken;
import com.odinsync.identity.application.model.RotatedRefreshToken;
import com.odinsync.identity.application.port.out.RefreshTokenGeneratorPort;
import com.odinsync.identity.application.port.out.RefreshTokenHasherPort;
import com.odinsync.identity.application.port.out.RefreshTokenRepositoryPort;
import com.odinsync.identity.domain.exception.InvalidRefreshTokenException;
import com.odinsync.identity.domain.exception.RefreshTokenReuseDetectedException;
import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.shared.security.RefreshTokenProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-19T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	private RefreshTokenGeneratorPort tokenGenerator;

	@Mock
	private RefreshTokenHasherPort tokenHasher;

	@Mock
	private RefreshTokenRepositoryPort refreshTokenRepository;

	@Test
	void issueStoresOnlyHashedTokenAndReturnsRawTokenOnce() {
		RefreshTokenService refreshTokenService = refreshTokenService();
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		when(tokenGenerator.generate()).thenReturn("raw-refresh-token");
		when(tokenHasher.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IssuedRefreshToken result = refreshTokenService.issue(userId, tenantId);

		assertThat(result.rawToken()).isEqualTo("raw-refresh-token");
		assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		verify(refreshTokenRepository).save(argThat(refreshToken ->
				refreshToken.userId().equals(userId)
						&& refreshToken.tenantId().equals(tenantId)
						&& refreshToken.tokenHash().equals("hashed-refresh-token")
						&& refreshToken.revokedAt() == null));
	}

	@Test
	void rotateRevokesCurrentTokenAndIssuesReplacementInSameFamily() {
		RefreshTokenService refreshTokenService = refreshTokenService();
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		UUID familyId = UUID.randomUUID();
		RefreshToken current = activeToken(userId, tenantId, familyId);
		when(tokenHasher.hash("current-raw-token")).thenReturn("current-token-hash");
		when(refreshTokenRepository.findByTokenHashForUpdate("current-token-hash"))
				.thenReturn(Optional.of(current));
		when(tokenGenerator.generate()).thenReturn("replacement-raw-token");
		when(tokenHasher.hash("replacement-raw-token")).thenReturn("replacement-token-hash");
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		RotatedRefreshToken result = refreshTokenService.rotate("current-raw-token");

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.tenantId()).isEqualTo(tenantId);
		assertThat(result.rawRefreshToken()).isEqualTo("replacement-raw-token");
		assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));

		ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(savedTokens.capture());
		assertThat(savedTokens.getAllValues().get(0).familyId()).isEqualTo(familyId);
		assertThat(savedTokens.getAllValues().get(1).revokedAt()).isEqualTo(NOW);
		assertThat(savedTokens.getAllValues().get(1).replacedByTokenId())
				.isEqualTo(savedTokens.getAllValues().get(0).id());
	}

	@Test
	void revokedTokenReuseRevokesActiveFamilyAndFails() {
		RefreshTokenService refreshTokenService = refreshTokenService();
		UUID familyId = UUID.randomUUID();
		RefreshToken revokedToken = activeToken(UUID.randomUUID(), UUID.randomUUID(), familyId);
		revokedToken.replaceWith(UUID.randomUUID(), NOW);
		when(tokenHasher.hash("reused-token")).thenReturn("reused-token-hash");
		when(refreshTokenRepository.findByTokenHashForUpdate("reused-token-hash"))
				.thenReturn(Optional.of(revokedToken));

		assertThatThrownBy(() -> refreshTokenService.rotate("reused-token"))
				.isInstanceOf(RefreshTokenReuseDetectedException.class);

		verify(refreshTokenRepository).revokeFamily(familyId, NOW);
	}

	@Test
	void expiredTokenIsRevokedAndFails() {
		RefreshTokenService refreshTokenService = refreshTokenService();
		RefreshToken expiredToken = new RefreshToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"expired-token-hash",
				UUID.randomUUID(),
				null,
				NOW.minus(Duration.ofDays(31)),
				NOW.minusSeconds(1),
				null,
				null,
				null,
				null,
				null,
				NOW.minus(Duration.ofDays(31)),
				NOW.minus(Duration.ofDays(31)),
				0);
		when(tokenHasher.hash("expired-token")).thenReturn("expired-token-hash");
		when(refreshTokenRepository.findByTokenHashForUpdate("expired-token-hash"))
				.thenReturn(Optional.of(expiredToken));

		assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
				.isInstanceOf(InvalidRefreshTokenException.class);

		verify(refreshTokenRepository).save(argThat(refreshToken -> refreshToken.revokedAt().equals(NOW)));
	}

	private static RefreshToken activeToken(UUID userId, UUID tenantId, UUID familyId) {
		return new RefreshToken(
				UUID.randomUUID(),
				userId,
				tenantId,
				"current-token-hash",
				familyId,
				null,
				NOW.minusSeconds(60),
				NOW.plus(Duration.ofDays(1)),
				null,
				null,
				null,
				null,
				null,
				NOW.minusSeconds(60),
				NOW.minusSeconds(60),
				0);
	}

	private RefreshTokenService refreshTokenService() {
		return new RefreshTokenService(
				tokenGenerator,
				tokenHasher,
				refreshTokenRepository,
				new RefreshTokenProperties(
						Duration.ofDays(30),
						64,
						Duration.ofDays(90),
						Duration.ofSeconds(3),
						3),
				CLOCK);
	}
}
