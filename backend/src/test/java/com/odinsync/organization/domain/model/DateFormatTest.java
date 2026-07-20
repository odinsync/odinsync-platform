package com.odinsync.organization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DateFormatTest {

	@Test
	void exposesExactStableValuesAndPatterns() {
		assertThat(DateFormat.values()).containsExactly(
				DateFormat.DD_MM_YYYY,
				DateFormat.MM_DD_YYYY,
				DateFormat.YYYY_MM_DD);
		assertThat(DateFormat.DD_MM_YYYY.name()).isEqualTo("DD_MM_YYYY");
		assertThat(DateFormat.MM_DD_YYYY.displayPattern()).isEqualTo("MM/dd/yyyy");
	}
}
