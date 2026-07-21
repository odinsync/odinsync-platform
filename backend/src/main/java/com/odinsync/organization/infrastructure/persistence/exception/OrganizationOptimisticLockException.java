package com.odinsync.organization.infrastructure.persistence.exception;

public class OrganizationOptimisticLockException extends OrganizationPersistenceException {

	public OrganizationOptimisticLockException(String message, Throwable cause) {
		super(message, cause);
	}
}
