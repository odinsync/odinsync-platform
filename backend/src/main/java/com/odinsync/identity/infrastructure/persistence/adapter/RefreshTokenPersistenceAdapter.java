package com.odinsync.identity.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.port.out.RefreshTokenRepositoryPort;
import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.identity.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.odinsync.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

	private final RefreshTokenJpaRepository repository;
	private final RefreshTokenPersistenceMapper mapper;

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return mapper.toDomain(repository.save(mapper.toEntity(refreshToken)));
	}

	@Override
	public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
		return repository.findByTokenHashForUpdate(tokenHash)
				.map(mapper::toDomain);
	}

	@Override
	public void revokeActiveFamily(UUID familyId, Instant revokedAt) {
		repository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
				.forEach(refreshToken -> {
					refreshToken.revoke(revokedAt, null);
					repository.save(refreshToken);
				});
	}
}
