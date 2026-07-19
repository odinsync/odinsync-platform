package com.odinsync.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

	/**
	 * Finds a refresh-token row by its SHA-256 token hash.
	 */
	Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

	/**
	 * Finds a refresh-token row with a pessimistic write lock for atomic rotation.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select refreshToken
			from RefreshTokenJpaEntity refreshToken
			where refreshToken.tokenHash = :tokenHash
			""")
	Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	/**
	 * Finds non-revoked rows in one token family for family-wide revocation.
	 */
	List<RefreshTokenJpaEntity> findAllByFamilyIdAndRevokedAtIsNull(UUID familyId);

	/**
	 * Finds active session rows for one user and tenant.
	 */
	@Query("""
			select refreshToken
			from RefreshTokenJpaEntity refreshToken
			where refreshToken.userId = :userId
			  and refreshToken.tenantId = :tenantId
			  and refreshToken.revokedAt is null
			  and refreshToken.replacedByTokenId is null
			  and refreshToken.expiresAt > :now
			order by refreshToken.issuedAt desc
			""")
	List<RefreshTokenJpaEntity> findActiveSessions(
			@Param("userId") UUID userId,
			@Param("tenantId") UUID tenantId,
			@Param("now") java.time.Instant now);

	/**
	 * Deletes old revoked rows that are past the retention cutoff.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			delete from RefreshTokenJpaEntity refreshToken
			where refreshToken.expiresAt < :cutoff
			  and refreshToken.revokedAt is not null
			""")
	int deleteExpiredAndRevokedBefore(@Param("cutoff") java.time.Instant cutoff);
}
