package com.odinsync.organization.application.exception;

import java.util.UUID;

public class OrganizationAlreadyExistsException extends RuntimeException {

	public OrganizationAlreadyExistsException(UUID tenantId) {
		super("Organization already exists for tenant: " + tenantId);
	}
}
