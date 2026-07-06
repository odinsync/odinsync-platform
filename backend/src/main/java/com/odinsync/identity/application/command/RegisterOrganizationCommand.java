package com.odinsync.identity.application.command;

public record RegisterOrganizationCommand(
		String organizationName,
		String legalName,
		String ownerName,
		String email,
		String password) {

	public String normalEmail() {
        return email.trim().toLowerCase();
	}
}
