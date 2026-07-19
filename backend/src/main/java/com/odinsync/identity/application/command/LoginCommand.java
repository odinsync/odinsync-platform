package com.odinsync.identity.application.command;

import java.util.Locale;

import com.odinsync.identity.application.model.SessionMetadata;

public record LoginCommand(
		String email,
		String password,
		SessionMetadata metadata) {

	/**
	 * Creates a login command when request metadata is unavailable.
	 */
	public LoginCommand(String email, String password) {
		this(email, password, SessionMetadata.empty());
	}

	/**
	 * Normalizes the email address before credential authentication.
	 */
	public String normalizedEmail() {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
