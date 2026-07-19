package com.odinsync.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.odinsync.shared.security.RefreshTokenProperties;
import org.junit.jupiter.api.Test;

class SecureRefreshTokenGeneratorTest {

	@Test
	void generatesUrlSafeTokenWithoutPadding() {
		SecureRefreshTokenGenerator generator = new SecureRefreshTokenGenerator(properties(64));

		String token = generator.generate();

		assertThat(token).isNotBlank();
		assertThat(token).doesNotContain("=");
		assertThat(token).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void repeatedTokensAreDifferent() {
		SecureRefreshTokenGenerator generator = new SecureRefreshTokenGenerator(properties(64));

		assertThat(generator.generate()).isNotEqualTo(generator.generate());
	}

	@Test
	void configuredByteLengthControlsEncodedLength() {
		SecureRefreshTokenGenerator generator = new SecureRefreshTokenGenerator(properties(32));

		assertThat(generator.generate()).hasSize(43);
	}

	private static RefreshTokenProperties properties(int tokenSizeBytes) {
		return new RefreshTokenProperties(
				Duration.ofDays(30),
				tokenSizeBytes,
				Duration.ofDays(90),
				Duration.ofSeconds(3),
				3);
	}
}
