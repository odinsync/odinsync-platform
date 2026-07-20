package com.odinsync.organization.application.exception;

public class OrganizationNotFoundException extends RuntimeException {

	public OrganizationNotFoundException() {
		super("Organization was not found");
	}
}
