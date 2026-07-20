package com.odinsync.organization.domain.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record EmailAddress(String value) {

	private static final int MAX_LENGTH = 254;
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
			Pattern.CASE_INSENSITIVE);

	public EmailAddress {
		value = OrganizationValueValidator.requiredTrimmed(value, "email", MAX_LENGTH)
				.toLowerCase(Locale.ROOT);
		if (value.contains(" ") || !EMAIL_PATTERN.matcher(value).matches()) {
			throw new InvalidOrganizationValueException("email must be a valid email address");
		}
	}
}
