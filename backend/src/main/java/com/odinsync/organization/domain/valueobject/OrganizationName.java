package com.odinsync.organization.domain.valueobject;

public record OrganizationName(
		String legalName,
		String displayName
) {
	private static final int LEGAL_NAME_MAX_LENGTH = 200;
	private static final int DISPLAY_NAME_MAX_LENGTH = 120;

	public OrganizationName {
		legalName = OrganizationValueValidator.requiredTrimmed(legalName, "legalName", LEGAL_NAME_MAX_LENGTH);
		displayName = OrganizationValueValidator.requiredTrimmed(displayName, "displayName", DISPLAY_NAME_MAX_LENGTH);
	}
}
