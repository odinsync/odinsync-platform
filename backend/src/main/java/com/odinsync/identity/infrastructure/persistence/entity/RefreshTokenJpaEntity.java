package com.odinsync.identity.infrastructure.persistence.entity;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

	@Id
	@Column(nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "token_hash", length = 64, nullable = false)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id")
	private UUID replacedByTokenId;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "device_name", length = 255)
	private String deviceName;

	@Column(name = "user_agent", length = 512)
	private String userAgent;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	/**
	 * Returns whether the persisted token is expired at the supplied clock instant.
	 */
	public boolean isExpired(Clock clock) {
		return !expiresAt.isAfter(clock.instant());
	}

	/**
	 * Returns whether the persisted token has been revoked.
	 */
	public boolean isRevoked() {
		return revokedAt != null;
	}

	/**
	 * Returns whether the persisted token can still represent an active session.
	 */
	public boolean isActive(Clock clock) {
		return !isRevoked() && !isExpired(clock) && replacedByTokenId == null;
	}

	/**
	 * Marks the persisted token as revoked and optionally links its replacement.
	 */
	public void revoke(Instant revokedAt, UUID replacementTokenId) {
		this.revokedAt = revokedAt;
		this.replacedByTokenId = replacementTokenId;
		this.updatedAt = revokedAt;
	}

	/**
	 * Initializes audit timestamps before the token row is first inserted.
	 */
	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	/**
	 * Refreshes the update timestamp whenever JPA updates the token row.
	 */
	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
