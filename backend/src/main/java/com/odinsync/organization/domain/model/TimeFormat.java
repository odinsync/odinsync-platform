package com.odinsync.organization.domain.model;

public enum TimeFormat {
	TWELVE_HOUR("hh:mm a"),
	TWENTY_FOUR_HOUR("HH:mm");

	private final String displayPattern;

	TimeFormat(String displayPattern) {
		this.displayPattern = displayPattern;
	}

	public String displayPattern() {
		return displayPattern;
	}
}
