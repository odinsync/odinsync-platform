package com.odinsync.organization.domain.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record OrganizationLocale(String value) {

	private static final Pattern BCP_47_SHAPE = Pattern.compile("^[A-Za-z]{2,8}(-[A-Za-z0-9]{2,8})*$");

	public OrganizationLocale {
		String normalizedInput = OrganizationValueValidator.requiredTrimmed(value, "locale", 20);
		if (!BCP_47_SHAPE.matcher(normalizedInput).matches()) {
			throw new InvalidOrganizationValueException("locale must be a valid BCP 47 language tag");
		}
		Locale locale = Locale.forLanguageTag(normalizedInput);
		if (locale.getLanguage().isBlank() || locale.getLanguage().equalsIgnoreCase("und")) {
			throw new InvalidOrganizationValueException("locale must define a valid language");
		}
		value = locale.toLanguageTag();
	}
}
