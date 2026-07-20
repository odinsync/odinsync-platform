package com.odinsync.organization.domain.model;

import java.time.DayOfWeek;

public enum WeekStart {
	MONDAY(DayOfWeek.MONDAY),
	SUNDAY(DayOfWeek.SUNDAY),
	SATURDAY(DayOfWeek.SATURDAY);

	private final DayOfWeek dayOfWeek;

	WeekStart(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public DayOfWeek dayOfWeek() {
		return dayOfWeek;
	}
}
