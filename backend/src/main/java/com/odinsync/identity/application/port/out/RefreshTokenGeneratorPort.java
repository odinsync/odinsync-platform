package com.odinsync.identity.application.port.out;

public interface RefreshTokenGeneratorPort {

	/**
	 * Generates a cryptographically strong opaque refresh-token secret.
	 */
	String generate();
}
