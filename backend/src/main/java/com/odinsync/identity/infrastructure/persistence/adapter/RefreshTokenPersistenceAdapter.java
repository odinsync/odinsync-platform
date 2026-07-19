package com.odinsync.identity.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.port.out.RefreshTokenRepositoryPort;
import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.odinsync.identity.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.odinsync.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

	private final RefreshTokenJpaRepository repository;
	private final RefreshTokenPersistenceMapper mapper;

	/**
	 * Persists a refresh-token domain model through the JPA entity mapper.
	 */
	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return mapper.toDomain(repository.save(mapper.toEntity(refreshToken)));
	}

	/**
	 * Finds a token by hash without locking, intended for read-only lookup scenarios.
	 */
	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return repository.findByTokenHash(tokenHash)
				.map(mapper::toDomain);
	}

	/**
	 * Finds a token by hash using a pessimistic database lock for atomic rotation.
	 */
	@Override
	public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
		return repository.findByTokenHashForUpdate(tokenHash)
				.map(mapper::toDomain);
	}

	/**
	 * Returns active, unexpired sessions for one user within one tenant.
	 */
	@Override
	public List<RefreshToken> findActiveSessions(UUID userId, UUID tenantId, Instant now) {
		return repository.findActiveSessions(userId, tenantId, now)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}

	/**
	 * Revokes every active token in one refresh-token family.
	 */
	@Override
	public void revokeFamily(UUID familyId, Instant revokedAt) {
		List<RefreshTokenJpaEntity> activeFamily = repository.findAllByFamilyIdAndRevokedAtIsNull(familyId);
		activeFamily.forEach(refreshToken -> {
			refreshToken.revoke(revokedAt, null);
			repository.save(refreshToken);
		});
	}

	/**
	 * Revokes all active refresh-token sessions for one user within one tenant.
	 */
	@Override
	public void revokeAllForUserAndTenant(UUID userId, UUID tenantId, Instant revokedAt) {
		List<RefreshTokenJpaEntity> activeSessions = repository.findActiveSessions(userId, tenantId, revokedAt);
		activeSessions.forEach(refreshToken -> {
			refreshToken.revoke(revokedAt, null);
			repository.save(refreshToken);
		});
	}

	/**
	 * Deletes old revoked records after the retention window has elapsed.
	 */
	@Override
	public int deleteExpiredAndRevokedBefore(Instant cutoff) {
		return repository.deleteExpiredAndRevokedBefore(cutoff);
	}
}
