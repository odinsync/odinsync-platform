package com.odinsync.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

	Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select refreshToken
			from RefreshTokenJpaEntity refreshToken
			where refreshToken.tokenHash = :tokenHash
			""")
	Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<RefreshTokenJpaEntity> findAllByFamilyIdAndRevokedAtIsNull(UUID familyId);
}
