package com.odinsync.organization.application.exception;

public class TenantContextMismatchException extends RuntimeException {

	public TenantContextMismatchException() {
		super("Organization does not belong to the authenticated tenant");
	}
}
