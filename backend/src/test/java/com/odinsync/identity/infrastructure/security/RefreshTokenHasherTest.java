package com.odinsync.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

	private final RefreshTokenHasher hasher = new RefreshTokenHasher();

	@Test
	void sameInputProducesSameLowercaseSha256Hash() {
		String hash = hasher.hash("refresh-token");

		assertThat(hash).isEqualTo(hasher.hash("refresh-token"));
		assertThat(hash).hasSize(64);
		assertThat(hash).matches("[0-9a-f]{64}");
	}

	@Test
	void differentInputsProduceDifferentHashes() {
		assertThat(hasher.hash("refresh-token-1"))
				.isNotEqualTo(hasher.hash("refresh-token-2"));
	}

	@Test
	void blankTokenIsRejected() {
		assertThatThrownBy(() -> hasher.hash(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
