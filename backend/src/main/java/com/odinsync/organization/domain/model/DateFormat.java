package com.odinsync.organization.domain.model;

public enum DateFormat {
	DD_MM_YYYY("dd/MM/yyyy"),
	MM_DD_YYYY("MM/dd/yyyy"),
	YYYY_MM_DD("yyyy-MM-dd");

	private final String displayPattern;

	DateFormat(String displayPattern) {
		this.displayPattern = displayPattern;
	}

	public String displayPattern() {
		return displayPattern;
	}
}
