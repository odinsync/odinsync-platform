package com.odinsync.organization.application.model;

public enum OrganizationPermission {

	PROFILE_READ("organization:read"),
	PROFILE_UPDATE("organization:update"),
	SETTINGS_READ("organization:settings:read"),
	SETTINGS_UPDATE("organization:settings:update");

	private final String value;

	OrganizationPermission(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
