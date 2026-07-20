package com.odinsync.organization.application.exception;

public class MissingTenantContextException extends RuntimeException {

	public MissingTenantContextException() {
		super("Authenticated tenant context is missing");
	}
}
