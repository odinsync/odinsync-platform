package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;
import org.junit.jupiter.api.Test;

class OrganizationSettingsTest {

	@Test
	void createsValidSettings() {
		OrganizationSettings settings = validSettings();

		assertThat(settings.currencyCode()).isEqualTo(new CurrencyCode("INR"));
		assertThat(settings.timeZone()).isEqualTo(new OrganizationTimeZone("Asia/Kolkata"));
		assertThat(settings.locale()).isEqualTo(new OrganizationLocale("en-IN"));
	}

	@Test
	void rejectsNullComponents() {
		assertThatThrownBy(() -> new OrganizationSettings(null, new OrganizationTimeZone("Asia/Kolkata"), new OrganizationLocale("en-IN"), DateFormat.DD_MM_YYYY, TimeFormat.TWENTY_FOUR_HOUR, WeekStart.MONDAY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettings(new CurrencyCode("INR"), null, new OrganizationLocale("en-IN"), DateFormat.DD_MM_YYYY, TimeFormat.TWENTY_FOUR_HOUR, WeekStart.MONDAY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettings(new CurrencyCode("INR"), new OrganizationTimeZone("Asia/Kolkata"), null, DateFormat.DD_MM_YYYY, TimeFormat.TWENTY_FOUR_HOUR, WeekStart.MONDAY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettings(new CurrencyCode("INR"), new OrganizationTimeZone("Asia/Kolkata"), new OrganizationLocale("en-IN"), null, TimeFormat.TWENTY_FOUR_HOUR, WeekStart.MONDAY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettings(new CurrencyCode("INR"), new OrganizationTimeZone("Asia/Kolkata"), new OrganizationLocale("en-IN"), DateFormat.DD_MM_YYYY, null, WeekStart.MONDAY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationSettings(new CurrencyCode("INR"), new OrganizationTimeZone("Asia/Kolkata"), new OrganizationLocale("en-IN"), DateFormat.DD_MM_YYYY, TimeFormat.TWENTY_FOUR_HOUR, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(validSettings()).isEqualTo(validSettings());
	}

	private static OrganizationSettings validSettings() {
		return new OrganizationSettings(
				new CurrencyCode("INR"),
				new OrganizationTimeZone("Asia/Kolkata"),
				new OrganizationLocale("en-IN"),
				DateFormat.DD_MM_YYYY,
				TimeFormat.TWENTY_FOUR_HOUR,
				WeekStart.MONDAY);
	}
}
