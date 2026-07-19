package com.odinsync.identity.infrastructure.persistence.mapper;

import com.odinsync.identity.domain.model.RefreshToken;
import com.odinsync.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

	public RefreshTokenJpaEntity toEntity(RefreshToken refreshToken) {
		RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
		entity.setId(refreshToken.id());
		entity.setUserId(refreshToken.userId());
		entity.setTenantId(refreshToken.tenantId());
		entity.setTokenHash(refreshToken.tokenHash());
		entity.setFamilyId(refreshToken.familyId());
		entity.setIssuedAt(refreshToken.issuedAt());
		entity.setExpiresAt(refreshToken.expiresAt());
		entity.setRevokedAt(refreshToken.revokedAt());
		entity.setReplacedByTokenId(refreshToken.replacedByTokenId());
		entity.setCreatedAt(refreshToken.createdAt());
		entity.setUpdatedAt(refreshToken.updatedAt());
		return entity;
	}

	public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
		return new RefreshToken(
				entity.getId(),
				entity.getUserId(),
				entity.getTenantId(),
				entity.getTokenHash(),
				entity.getFamilyId(),
				entity.getIssuedAt(),
				entity.getExpiresAt(),
				entity.getRevokedAt(),
				entity.getReplacedByTokenId(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
