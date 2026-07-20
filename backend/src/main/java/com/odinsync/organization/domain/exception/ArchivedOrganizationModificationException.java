package com.odinsync.organization.domain.exception;

public class ArchivedOrganizationModificationException extends OrganizationStateConflictException {

	public ArchivedOrganizationModificationException() {
		super("Archived organization cannot be modified");
	}
}
