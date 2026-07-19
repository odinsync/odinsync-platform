package com.odinsync.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.odinsync.identity.application.port.out.RefreshTokenHasherPort;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenHasher implements RefreshTokenHasherPort {

	/**
	 * Hashes a raw refresh token deterministically for indexed database lookup.
	 */
	@Override
	public String hash(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("Refresh token must not be blank");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] result = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(result);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
		}
	}
}
