package com.odinsync.organization.domain.valueobject;

import java.util.Locale;
import java.util.Optional;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record Address(
		String addressLine1,
		String addressLine2,
		String city,
		String stateOrRegion,
		String postalCode,
		String countryCode
) {
	private static final int ADDRESS_LINE_MAX_LENGTH = 200;
	private static final int CITY_MAX_LENGTH = 100;
	private static final int STATE_OR_REGION_MAX_LENGTH = 100;
	private static final int POSTAL_CODE_MAX_LENGTH = 20;

	public Address {
		addressLine1 = OrganizationValueValidator.requiredTrimmed(
				addressLine1,
				"addressLine1",
				ADDRESS_LINE_MAX_LENGTH);
		addressLine2 = OrganizationValueValidator.optionalTrimmed(
				addressLine2,
				"addressLine2",
				ADDRESS_LINE_MAX_LENGTH);
		city = OrganizationValueValidator.requiredTrimmed(city, "city", CITY_MAX_LENGTH);
		stateOrRegion = OrganizationValueValidator.requiredTrimmed(
				stateOrRegion,
				"stateOrRegion",
				STATE_OR_REGION_MAX_LENGTH);
		postalCode = OrganizationValueValidator.requiredTrimmed(postalCode, "postalCode", POSTAL_CODE_MAX_LENGTH);
		countryCode = normalizeCountryCode(countryCode);
	}

	public Optional<String> addressLine2Value() {
		return Optional.ofNullable(addressLine2);
	}

	private static String normalizeCountryCode(String value) {
		String normalized = OrganizationValueValidator.requiredTrimmed(value, "countryCode", 2)
				.toUpperCase(Locale.ROOT);
		if (normalized.length() != 2 || !normalized.chars().allMatch(Character::isLetter)) {
			throw new InvalidOrganizationValueException("countryCode must be a two-letter ISO 3166-1 alpha-2 code");
		}
		return normalized;
	}
}
