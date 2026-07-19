package com.odinsync.identity.presentation.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
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
