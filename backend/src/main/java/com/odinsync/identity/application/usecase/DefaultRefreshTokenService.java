package com.odinsync.identity.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.model.ActiveSession;
import com.odinsync.identity.application.model.IssuedRefreshToken;
import com.odinsync.identity.application.model.RotatedRefreshToken;
import com.odinsync.identity.application.model.SessionMetadata;
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
public class DefaultRefreshTokenService implements RefreshTokenService {

	private final RefreshTokenGeneratorPort tokenGenerator;
	private final RefreshTokenHasherPort tokenHasher;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final RefreshTokenProperties properties;
	private final Clock clock;

	/**
	 * Creates the first refresh-token session for a login using a new token family.
	 */
	@Override
	@Transactional
	public IssuedRefreshToken issue(UUID userId, UUID tenantId) {
		return issue(userId, tenantId, UUID.randomUUID(), SessionMetadata.empty());
	}

	/**
	 * Issues a refresh token in the supplied family without request metadata.
	 */
	@Override
	@Transactional
	public IssuedRefreshToken issue(UUID userId, UUID tenantId, UUID familyId) {
		return issue(userId, tenantId, familyId, SessionMetadata.empty());
	}

	/**
	 * Generates an opaque refresh token, stores only its hash, and returns the raw token once.
	 */
	@Override
	@Transactional
	public IssuedRefreshToken issue(UUID userId, UUID tenantId, UUID familyId, SessionMetadata metadata) {
		Instant now = clock.instant();
		UUID tokenId = UUID.randomUUID();
		String rawToken = tokenGenerator.generate();
		RefreshToken refreshToken = new RefreshToken(
				tokenId,
				userId,
				tenantId,
				tokenHasher.hash(rawToken),
				familyId,
				null,
				now,
				now.plus(properties.ttl()),
				null,
				null,
				metadata.deviceName(),
				metadata.userAgent(),
				metadata.ipAddress(),
				now,
				now,
				0);

		RefreshToken saved = refreshTokenRepository.save(refreshToken);
		return new IssuedRefreshToken(tokenId, familyId, rawToken, saved.expiresAt(), saved);
	}

	/**
	 * Rotates a refresh token without session metadata, preserving the token family.
	 */
	@Override
	@Transactional
	public RotatedRefreshToken rotate(String rawRefreshToken) {
		return rotate(rawRefreshToken, SessionMetadata.empty());
	}

	/**
	 * Atomically replaces one active refresh token with a new one and revokes the old token.
	 */
	@Override
	@Transactional
	public RotatedRefreshToken rotate(String rawRefreshToken, SessionMetadata metadata) {
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
				.orElseThrow(InvalidRefreshTokenException::new);

		Instant now = clock.instant();
		if (current.isRevoked() || current.hasBeenRotated()) {
			refreshTokenRepository.revokeFamily(current.familyId(), now);
			throw new RefreshTokenReuseDetectedException();
		}
		if (current.isExpired(now)) {
			current.revoke(now);
			refreshTokenRepository.save(current);
			throw new InvalidRefreshTokenException();
		}

		current.markUsed(now);
		IssuedRefreshToken replacement = issue(current.userId(), current.tenantId(), current.familyId(), metadata);

		current.replaceWith(replacement.id(), now);
		refreshTokenRepository.save(current);
		return new RotatedRefreshToken(
				current.userId(),
				current.tenantId(),
				replacement.rawToken(),
				replacement.expiresAt());
	}

	/**
	 * Revokes the token family for the submitted refresh token and remains idempotent.
	 */
	@Override
	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
				.ifPresent(refreshToken -> refreshTokenRepository.revokeFamily(refreshToken.familyId(), clock.instant()));
	}

	/**
	 * Revokes all active refresh-token sessions for a user within one tenant.
	 */
	@Override
	@Transactional
	public void logoutAll(UUID userId, UUID tenantId) {
		refreshTokenRepository.revokeAllForUserAndTenant(userId, tenantId, clock.instant());
	}

	/**
	 * Returns safe active-session metadata without exposing refresh-token secrets or hashes.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ActiveSession> getActiveSessions(UUID userId, UUID tenantId) {
		Instant now = clock.instant();
		return refreshTokenRepository.findActiveSessions(userId, tenantId, now)
				.stream()
				.map(refreshToken -> new ActiveSession(
						refreshToken.id(),
						refreshToken.familyId(),
						refreshToken.deviceName(),
						refreshToken.userAgent(),
						refreshToken.ipAddress(),
						refreshToken.issuedAt(),
						refreshToken.lastUsedAt(),
						refreshToken.expiresAt(),
						false))
				.toList();
	}

	/**
	 * Revokes one active session only when it belongs to the authenticated user and tenant.
	 */
	@Override
	@Transactional
	public void revokeSession(UUID sessionId, UUID authenticatedUserId, UUID tenantId) {
		Instant now = clock.instant();
		refreshTokenRepository.findActiveSessions(authenticatedUserId, tenantId, now)
				.stream()
				.filter(refreshToken -> refreshToken.id().equals(sessionId))
				.findFirst()
				.ifPresent(refreshToken -> refreshTokenRepository.revokeFamily(refreshToken.familyId(), now));
	}

	/**
	 * Deletes expired, revoked refresh-token records older than the configured retention period.
	 */
	@Override
	@Transactional
	public int cleanupExpiredTokens() {
		Instant cutoff = clock.instant().minus(properties.retentionPeriod());
		return refreshTokenRepository.deleteExpiredAndRevokedBefore(cutoff);
	}
}
