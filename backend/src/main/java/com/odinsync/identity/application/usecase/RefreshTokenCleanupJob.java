package com.odinsync.identity.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RefreshTokenCleanupJob {

	private final RefreshTokenService refreshTokenService;

	/**
	 * Runs the refresh-token retention cleanup on a configurable schedule.
	 *
	 * <p>The default cron expression runs daily at 02:30 server time. The service
	 * deletes only expired refresh-token records that were already revoked and are
	 * older than the configured retention period, so active sessions and recent
	 * audit history remain intact.
	 */
	@Scheduled(cron = "${odinsync.security.refresh-token.cleanup-cron:0 30 2 * * *}")
	void cleanupExpiredTokens() {
		refreshTokenService.cleanupExpiredTokens();
	}
}
