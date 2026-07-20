package com.odinsync.organization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;

class WeekStartTest {

	@Test
	void exposesExactStableValuesAndDayMappings() {
		assertThat(WeekStart.values()).containsExactly(
				WeekStart.MONDAY,
				WeekStart.SUNDAY,
				WeekStart.SATURDAY);
		assertThat(WeekStart.MONDAY.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
		assertThat(WeekStart.SUNDAY.name()).isEqualTo("SUNDAY");
	}
}
