package com.odinsync.organization.domain.valueobject;

import java.util.Objects;

import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;

public record OrganizationSettings(
		CurrencyCode currencyCode,
		OrganizationTimeZone timeZone,
		OrganizationLocale locale,
		DateFormat dateFormat,
		TimeFormat timeFormat,
		WeekStart weekStart
) {
	public OrganizationSettings {
		currencyCode = Objects.requireNonNull(currencyCode, "currencyCode must not be null");
		timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
		locale = Objects.requireNonNull(locale, "locale must not be null");
		dateFormat = Objects.requireNonNull(dateFormat, "dateFormat must not be null");
		timeFormat = Objects.requireNonNull(timeFormat, "timeFormat must not be null");
		weekStart = Objects.requireNonNull(weekStart, "weekStart must not be null");
	}
}
