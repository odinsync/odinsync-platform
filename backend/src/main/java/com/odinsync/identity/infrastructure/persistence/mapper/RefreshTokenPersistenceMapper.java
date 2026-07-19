package com.odinsync.identity.infrastructure.persistence.mapper;

import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

	/**
	 * Converts the domain refresh-token model into its JPA persistence shape.
	 */
	public RefreshTokenJpaEntity toEntity(RefreshToken refreshToken) {
		RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
		entity.setId(refreshToken.id());
		entity.setUserId(refreshToken.userId());
		entity.setTenantId(refreshToken.tenantId());
		entity.setTokenHash(refreshToken.tokenHash());
		entity.setFamilyId(refreshToken.familyId());
		entity.setReplacedByTokenId(refreshToken.replacedByTokenId());
		entity.setIssuedAt(refreshToken.issuedAt());
		entity.setExpiresAt(refreshToken.expiresAt());
		entity.setRevokedAt(refreshToken.revokedAt());
		entity.setLastUsedAt(refreshToken.lastUsedAt());
		entity.setDeviceName(refreshToken.deviceName());
		entity.setUserAgent(refreshToken.userAgent());
		entity.setIpAddress(refreshToken.ipAddress());
		entity.setCreatedAt(refreshToken.createdAt());
		entity.setUpdatedAt(refreshToken.updatedAt());
		entity.setVersion(refreshToken.version());
		return entity;
	}

	/**
	 * Converts the JPA refresh-token entity back into the domain model.
	 */
	public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
		return new RefreshToken(
				entity.getId(),
				entity.getUserId(),
				entity.getTenantId(),
				entity.getTokenHash(),
				entity.getFamilyId(),
				entity.getReplacedByTokenId(),
				entity.getIssuedAt(),
				entity.getExpiresAt(),
				entity.getRevokedAt(),
				entity.getLastUsedAt(),
				entity.getDeviceName(),
				entity.getUserAgent(),
				entity.getIpAddress(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}
}
