package com.odinsync.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

	private final UUID id;
	private final UUID userId;
	private final UUID tenantId;
	private final String tokenHash;
	private final UUID familyId;
	private UUID replacedByTokenId;
	private final Instant issuedAt;
	private final Instant expiresAt;
	private Instant revokedAt;
	private Instant lastUsedAt;
	private final String deviceName;
	private final String userAgent;
	private final String ipAddress;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	/**
	 * Creates a refresh-token domain model and validates core token invariants.
	 */
	public RefreshToken(
			UUID id,
			UUID userId,
			UUID tenantId,
			String tokenHash,
			UUID familyId,
			UUID replacedByTokenId,
			Instant issuedAt,
			Instant expiresAt,
			Instant revokedAt,
			Instant lastUsedAt,
			String deviceName,
			String userAgent,
			String ipAddress,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = requireNonNull(id, "id");
		this.userId = requireNonNull(userId, "userId");
		this.tenantId = requireNonNull(tenantId, "tenantId");
		this.tokenHash = requireNotBlank(tokenHash, "tokenHash");
		this.familyId = requireNonNull(familyId, "familyId");
		this.issuedAt = requireNonNull(issuedAt, "issuedAt");
		this.expiresAt = requireNonNull(expiresAt, "expiresAt");
		this.createdAt = requireNonNull(createdAt, "createdAt");
		this.updatedAt = requireNonNull(updatedAt, "updatedAt");
		if (issuedAt.isAfter(expiresAt)) {
			throw new IllegalArgumentException("issuedAt must not be after expiresAt");
		}
		if (revokedAt != null && revokedAt.isBefore(issuedAt)) {
			throw new IllegalArgumentException("revokedAt must not be before issuedAt");
		}
		if (id.equals(replacedByTokenId)) {
			throw new IllegalArgumentException("A refresh token cannot replace itself");
		}
		this.replacedByTokenId = replacedByTokenId;
		this.revokedAt = revokedAt;
		this.lastUsedAt = lastUsedAt;
		this.deviceName = deviceName;
		this.userAgent = userAgent;
		this.ipAddress = ipAddress;
		this.version = version;
	}

	/**
	 * Returns whether the token has reached or passed its expiration instant.
	 */
	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	/**
	 * Returns whether the token has been explicitly revoked.
	 */
	public boolean isRevoked() {
		return revokedAt != null;
	}

	/**
	 * Returns whether the token can still be used for refresh rotation.
	 */
	public boolean isActive(Instant now) {
		return !isRevoked() && !isExpired(now) && !hasBeenRotated();
	}

	/**
	 * Returns whether this token has already been replaced by another token.
	 */
	public boolean hasBeenRotated() {
		return replacedByTokenId != null;
	}

	/**
	 * Revokes the token without assigning a replacement token.
	 */
	public void revoke(Instant now) {
		ensureNotBeforeIssuedAt(now, "revokedAt");
		if (revokedAt == null) {
			revokedAt = now;
			updatedAt = now;
		}
	}

	/**
	 * Marks this token as replaced and revoked in the same state transition.
	 */
	public void replaceWith(UUID replacementTokenId, Instant now) {
		if (id.equals(replacementTokenId)) {
			throw new IllegalArgumentException("A refresh token cannot replace itself");
		}
		ensureNotBeforeIssuedAt(now, "revokedAt");
		replacedByTokenId = requireNonNull(replacementTokenId, "replacementTokenId");
		revokedAt = now;
		updatedAt = now;
	}

	/**
	 * Records the last successful use time for audit and session visibility.
	 */
	public void markUsed(Instant now) {
		ensureNotBeforeIssuedAt(now, "lastUsedAt");
		lastUsedAt = now;
		updatedAt = now;
	}

	/**
	 * Returns the refresh-token record id.
	 */
	public UUID id() {
		return id;
	}

	/**
	 * Returns the user who owns this refresh-token session.
	 */
	public UUID userId() {
		return userId;
	}

	/**
	 * Returns the tenant that scopes this refresh-token session.
	 */
	public UUID tenantId() {
		return tenantId;
	}

	/**
	 * Returns the persisted SHA-256 hash of the raw refresh token.
	 */
	public String tokenHash() {
		return tokenHash;
	}

	/**
	 * Returns the token-family id shared by rotations from one login session.
	 */
	public UUID familyId() {
		return familyId;
	}

	/**
	 * Returns the replacement token id when this token has been rotated.
	 */
	public UUID replacedByTokenId() {
		return replacedByTokenId;
	}

	/**
	 * Returns when this refresh token was issued.
	 */
	public Instant issuedAt() {
		return issuedAt;
	}

	/**
	 * Returns when this refresh token expires.
	 */
	public Instant expiresAt() {
		return expiresAt;
	}

	/**
	 * Returns when this refresh token was revoked, if applicable.
	 */
	public Instant revokedAt() {
		return revokedAt;
	}

	/**
	 * Returns when this refresh token was last used successfully.
	 */
	public Instant lastUsedAt() {
		return lastUsedAt;
	}

	/**
	 * Returns the optional device name captured for session display.
	 */
	public String deviceName() {
		return deviceName;
	}

	/**
	 * Returns the optional user-agent captured for session audit.
	 */
	public String userAgent() {
		return userAgent;
	}

	/**
	 * Returns the optional IP address captured for session audit.
	 */
	public String ipAddress() {
		return ipAddress;
	}

	/**
	 * Returns when the refresh-token record was created.
	 */
	public Instant createdAt() {
		return createdAt;
	}

	/**
	 * Returns when the refresh-token record was last updated.
	 */
	public Instant updatedAt() {
		return updatedAt;
	}

	/**
	 * Returns the persistence version used for defensive optimistic tracking.
	 */
	public long version() {
		return version;
	}

	/**
	 * Ensures a domain timestamp does not move before token issuance.
	 */
	private void ensureNotBeforeIssuedAt(Instant value, String fieldName) {
		requireNonNull(value, fieldName);
		if (value.isBefore(issuedAt)) {
			throw new IllegalArgumentException(fieldName + " must not be before issuedAt");
		}
	}

	/**
	 * Rejects missing required domain values.
	 */
	private static <T> T requireNonNull(T value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		return value;
	}

	/**
	 * Rejects blank required text values.
	 */
	private static String requireNotBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
