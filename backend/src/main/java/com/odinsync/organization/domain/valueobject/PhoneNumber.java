package com.odinsync.organization.domain.valueobject;

import java.util.regex.Pattern;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record PhoneNumber(String value) {

	private static final int MAX_LENGTH = 30;
	private static final Pattern ALLOWED_PATTERN = Pattern.compile("^\\+?[0-9()\\- ]+$");

	public PhoneNumber {
		value = OrganizationValueValidator.requiredTrimmed(value, "phone", MAX_LENGTH);
		if (!ALLOWED_PATTERN.matcher(value).matches()) {
			throw new InvalidOrganizationValueException("phone must contain only digits and supported separators");
		}
		if (value.chars().noneMatch(Character::isDigit)) {
			throw new InvalidOrganizationValueException("phone must contain at least one digit");
		}
	}
}
