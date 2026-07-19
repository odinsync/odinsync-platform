package com.odinsync.identity.application.port.out;

public interface RefreshTokenHasherPort {

	/**
	 * Hashes a raw refresh-token secret for deterministic database lookup.
	 */
	String hash(String rawToken);
}
