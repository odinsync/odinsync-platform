package com.odinsync.identity.application.command;

import com.odinsync.identity.application.model.SessionMetadata;

public record RefreshTokenCommand(
		String refreshToken,
		SessionMetadata metadata) {

	/**
	 * Creates a refresh command when request metadata is unavailable.
	 */
	public RefreshTokenCommand(String refreshToken) {
		this(refreshToken, SessionMetadata.empty());
	}
}
