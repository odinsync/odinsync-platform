package com.odinsync.identity.domain.exception;

public class InactiveTenantException extends RuntimeException {

	public InactiveTenantException() {
		super("Tenant is not active");
	}
}
