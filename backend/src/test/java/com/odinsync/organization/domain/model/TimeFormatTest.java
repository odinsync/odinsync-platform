package com.odinsync.organization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

	@Test
	void exposesExactStableValuesAndPatterns() {
		assertThat(TimeFormat.values()).containsExactly(
				TimeFormat.TWELVE_HOUR,
				TimeFormat.TWENTY_FOUR_HOUR);
		assertThat(TimeFormat.TWELVE_HOUR.name()).isEqualTo("TWELVE_HOUR");
		assertThat(TimeFormat.TWENTY_FOUR_HOUR.displayPattern()).isEqualTo("HH:mm");
	}
}
