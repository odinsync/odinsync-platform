package com.odinsync.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "odinsync.security.jwt")
public record OdinSyncJwtProperties(
		String issuer,
		Duration accessTokenTtl,
		String privateKeyLocation,
		String publicKeyLocation,
		boolean generateDevelopmentKeys) {

	public OdinSyncJwtProperties {
		if (issuer == null || issuer.isBlank()) {
			issuer = "odinsync-platform";
		}
		if (accessTokenTtl == null) {
			accessTokenTtl = Duration.ofMinutes(15);
		}
	}
}
