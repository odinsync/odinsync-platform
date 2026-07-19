package com.odinsync.identity.application.model;

import java.time.Instant;
import java.util.UUID;

public record ActiveSession(
		UUID sessionId,
		UUID familyId,
		String deviceName,
		String userAgent,
		String ipAddress,
		Instant issuedAt,
		Instant lastUsedAt,
		Instant expiresAt,
		boolean currentSession) {
}
