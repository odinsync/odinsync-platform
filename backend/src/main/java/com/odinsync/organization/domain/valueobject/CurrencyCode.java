package com.odinsync.organization.domain.valueobject;

import java.util.Currency;
import java.util.Locale;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record CurrencyCode(String value) {

	public CurrencyCode {
		value = OrganizationValueValidator.requiredTrimmed(value, "currencyCode", 3)
				.toUpperCase(Locale.ROOT);
		try {
			value = Currency.getInstance(value).getCurrencyCode();
		} catch (IllegalArgumentException exception) {
			throw new InvalidOrganizationValueException("currencyCode must be a valid ISO 4217 code");
		}
	}
}
