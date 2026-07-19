package com.odinsync.identity.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;

import com.odinsync.identity.application.port.out.RefreshTokenGeneratorPort;
import com.odinsync.shared.security.RefreshTokenProperties;
import org.springframework.stereotype.Component;

@Component
class SecureRefreshTokenGenerator implements RefreshTokenGeneratorPort {

	private final SecureRandom secureRandom;
	private final int tokenBytes;

	/**
	 * Creates a generator using the configured refresh-token entropy size.
	 */
	SecureRefreshTokenGenerator(RefreshTokenProperties properties) {
		this.secureRandom = new SecureRandom();
		this.tokenBytes = properties.tokenSizeBytes();
	}

	/**
	 * Generates a URL-safe opaque token value suitable for returning once to a client.
	 */
	@Override
	public String generate() {
		byte[] bytes = new byte[tokenBytes];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(bytes);
	}
}
