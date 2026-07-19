package com.odinsync.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "odinsync.security.refresh-token")
public record RefreshTokenProperties(
		@NotNull
		Duration ttl,
		@Min(32)
		int tokenSizeBytes,
		@NotNull
		Duration retentionPeriod,
		@NotNull
		Duration lockTimeout,
		@Positive
		int maxGenerationAttempts) {

	/**
	 * Applies secure defaults and rejects unsafe refresh-token configuration values.
	 */
	public RefreshTokenProperties {
		if (ttl == null) {
			ttl = Duration.ofDays(30);
		}
		if (!ttl.isPositive()) {
			throw new IllegalArgumentException("Refresh token ttl must be positive");
		}
		if (tokenSizeBytes == 0) {
			tokenSizeBytes = 64;
		}
		if (retentionPeriod == null) {
			retentionPeriod = Duration.ofDays(90);
		}
		if (retentionPeriod.isNegative()) {
			throw new IllegalArgumentException("Refresh token retention period must not be negative");
		}
		if (lockTimeout == null) {
			lockTimeout = Duration.ofSeconds(3);
		}
		if (!lockTimeout.isPositive()) {
			throw new IllegalArgumentException("Refresh token lock timeout must be positive");
		}
		if (maxGenerationAttempts == 0) {
			maxGenerationAttempts = 3;
		}
	}
}
