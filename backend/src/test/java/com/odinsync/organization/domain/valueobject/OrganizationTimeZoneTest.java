package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class OrganizationTimeZoneTest {

	@Test
	void acceptsValidIanaZones() {
		assertThat(new OrganizationTimeZone(" Asia/Kolkata ").value()).isEqualTo("Asia/Kolkata");
		assertThat(new OrganizationTimeZone("Europe/London").value()).isEqualTo("Europe/London");
	}

	@Test
	void rejectsInvalidTimezoneValues() {
		assertThatThrownBy(() -> new OrganizationTimeZone(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationTimeZone(" "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationTimeZone("Mars/Olympus"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationTimeZone("IST"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationTimeZone("+05:30"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new OrganizationTimeZone("Asia/Kolkata")).isEqualTo(new OrganizationTimeZone("Asia/Kolkata"));
	}
}
