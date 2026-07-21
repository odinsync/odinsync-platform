package com.odinsync.organization.application.service;

import java.util.Objects;

import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;
import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.CurrencyCode;
import com.odinsync.organization.domain.valueobject.OrganizationSettings;
import com.odinsync.organization.domain.valueobject.OrganizationLocale;
import com.odinsync.organization.domain.valueobject.OrganizationTimeZone;
import com.odinsync.organization.domain.valueobject.PhoneNumber;

public record OrganizationProvisioningDefaults(
		Address address,
		PhoneNumber phone,
		OrganizationSettings settings) {

	public OrganizationProvisioningDefaults {
		Objects.requireNonNull(address, "address must not be null");
		Objects.requireNonNull(phone, "phone must not be null");
		Objects.requireNonNull(settings, "settings must not be null");
	}

	public static OrganizationProvisioningDefaults of(
			String addressLine1,
			String addressLine2,
			String city,
			String stateOrRegion,
			String postalCode,
			String countryCode,
			String phone,
			String currency,
			String locale,
			String timeZone,
			String dateFormat,
			String timeFormat,
			String weekStart) {
		return new OrganizationProvisioningDefaults(
				new Address(
						defaultIfBlank(addressLine1, "Not provided"),
						addressLine2,
						defaultIfBlank(city, "Not provided"),
						defaultIfBlank(stateOrRegion, "Not provided"),
						defaultIfBlank(postalCode, "00000"),
						defaultIfBlank(countryCode, "ZZ")),
				new PhoneNumber(defaultIfNotProvided(phone, "0000000000")),
				new OrganizationSettings(
						new CurrencyCode(defaultIfNotProvided(currency, "INR")),
						new OrganizationTimeZone(defaultIfNotProvided(timeZone, "Asia/Kolkata")),
						new OrganizationLocale(defaultIfNotProvided(locale, "en-IN")),
						DateFormat.valueOf(defaultIfNotProvided(dateFormat, DateFormat.DD_MM_YYYY.name())),
						TimeFormat.valueOf(defaultIfNotProvided(timeFormat, TimeFormat.TWENTY_FOUR_HOUR.name())),
						WeekStart.valueOf(defaultIfNotProvided(weekStart, WeekStart.MONDAY.name()))));
	}

	private static String defaultIfBlank(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value;
	}

	private static String defaultIfNotProvided(String value, String fallback) {
		if (value == null || value.isBlank() || value.equalsIgnoreCase("Not provided")) {
			return fallback;
		}
		return value;
	}
}
