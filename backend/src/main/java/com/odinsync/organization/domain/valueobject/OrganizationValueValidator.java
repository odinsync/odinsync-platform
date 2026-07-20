package com.odinsync.organization.domain.valueobject;

import java.util.Objects;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

final class OrganizationValueValidator {

	private OrganizationValueValidator() {
	}

	static String requiredTrimmed(String value, String fieldName, int maxLength) {
		String trimmed = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (trimmed.isBlank()) {
			throw new InvalidOrganizationValueException(fieldName + " must not be blank");
		}
		if (trimmed.length() > maxLength) {
			throw new InvalidOrganizationValueException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	static String optionalTrimmed(String value, String fieldName, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isBlank()) {
			return null;
		}
		if (trimmed.length() > maxLength) {
			throw new InvalidOrganizationValueException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}
}
