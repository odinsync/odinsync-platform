package com.odinsync.identity.application.command;

import java.util.Locale;

public record LoginCommand(
		String email,
		String password) {

	public String normalizedEmail() {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
