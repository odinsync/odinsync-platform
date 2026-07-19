package com.odinsync.identity.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.odinsync.identity.application.port.out.RefreshTokenGeneratorPort;
import com.odinsync.identity.application.port.out.RefreshTokenHasherPort;
import com.odinsync.identity.application.port.out.RefreshTokenRepositoryPort;
import com.odinsync.identity.domain.exception.InvalidRefreshTokenException;
import com.odinsync.identity.domain.exception.RefreshTokenReuseDetectedException;
import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.shared.security.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenGeneratorPort tokenGenerator;
	private final RefreshTokenHasherPort tokenHasher;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final RefreshTokenProperties properties;
	private final Clock clock;

	@Transactional
	public IssuedRefreshToken issue(UUID userId, UUID tenantId) {
		return issue(userId, tenantId, UUID.randomUUID());
	}

	@Transactional
	public IssuedRefreshToken issue(UUID userId, UUID tenantId, UUID familyId) {
		Instant now = clock.instant();
		UUID tokenId = UUID.randomUUID();
		String rawToken = tokenGenerator.generate();
		RefreshToken refreshToken = new RefreshToken(
				tokenId,
				userId,
				tenantId,
				tokenHasher.hash(rawToken),
				familyId,
				now,
				now.plus(properties.ttl()),
				null,
				null,
				now,
				now);

		refreshTokenRepository.save(refreshToken);
		return new IssuedRefreshToken(tokenId, familyId, rawToken, refreshToken.expiresAt());
	}

	@Transactional
	public RotatedRefreshToken rotate(String rawRefreshToken) {
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
				.orElseThrow(InvalidRefreshTokenException::new);

		Instant now = clock.instant();
		if (current.isRevoked()) {
			refreshTokenRepository.revokeActiveFamily(current.familyId(), now);
			throw new RefreshTokenReuseDetectedException();
		}
		if (current.isExpired(clock)) {
			refreshTokenRepository.save(current.revoke(now, null));
			throw new InvalidRefreshTokenException();
		}

		IssuedRefreshToken replacement = issue(current.userId(), current.tenantId(), current.familyId());
		refreshTokenRepository.save(current.revoke(now, replacement.id()));
		return new RotatedRefreshToken(
				current.userId(),
				current.tenantId(),
				replacement.rawToken(),
				replacement.expiresAt());
	}
}
