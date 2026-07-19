package com.odinsync.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-07-19T00:00:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-08-18T00:00:00Z");

	@Test
	void activeTokenIsValidBeforeExpiration() {
		RefreshToken token = activeToken();

		assertThat(token.isActive(ISSUED_AT.plusSeconds(60))).isTrue();
	}

	@Test
	void expiredTokenIsNotActive() {
		RefreshToken token = activeToken();

		assertThat(token.isExpired(EXPIRES_AT)).isTrue();
		assertThat(token.isActive(EXPIRES_AT)).isFalse();
	}

	@Test
	void replacementMarksTokenRevokedAndRotated() {
		RefreshToken token = activeToken();
		UUID replacementId = UUID.randomUUID();

		token.replaceWith(replacementId, ISSUED_AT.plusSeconds(30));

		assertThat(token.isRevoked()).isTrue();
		assertThat(token.hasBeenRotated()).isTrue();
		assertThat(token.replacedByTokenId()).isEqualTo(replacementId);
		assertThat(token.isActive(ISSUED_AT.plusSeconds(31))).isFalse();
	}

	@Test
	void tokenCannotReplaceItself() {
		RefreshToken token = activeToken();

		assertThatThrownBy(() -> token.replaceWith(token.id(), ISSUED_AT.plusSeconds(30)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void invalidTimestampsAreRejected() {
		assertThatThrownBy(() -> new RefreshToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"token-hash",
				UUID.randomUUID(),
				null,
				EXPIRES_AT,
				ISSUED_AT,
				null,
				null,
				null,
				null,
				null,
				ISSUED_AT,
				ISSUED_AT,
				0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static RefreshToken activeToken() {
		return new RefreshToken(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"refresh-token-hash",
				UUID.randomUUID(),
				null,
				ISSUED_AT,
				EXPIRES_AT,
				null,
				null,
				null,
				null,
				null,
				ISSUED_AT,
				ISSUED_AT,
				0);
	}
}
