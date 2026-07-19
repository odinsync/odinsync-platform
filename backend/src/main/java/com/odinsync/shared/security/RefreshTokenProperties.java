package com.odinsync.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "odinsync.security.refresh-token")
public record RefreshTokenProperties(
		Duration ttl,
		int tokenBytes) {

	public RefreshTokenProperties {
		if (ttl == null) {
			ttl = Duration.ofDays(30);
		}
		if (tokenBytes <= 0) {
			tokenBytes = 64;
		}
	}
}
