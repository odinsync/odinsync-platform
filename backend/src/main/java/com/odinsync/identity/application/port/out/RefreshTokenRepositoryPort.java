package com.odinsync.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {

	RefreshToken save(RefreshToken refreshToken);

	Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

	void revokeActiveFamily(UUID familyId, Instant revokedAt);
}
