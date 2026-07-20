package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class AddressTest {

	@Test
	void createsCompleteValidAddressAndNormalizesCountryCode() {
		Address address = new Address(
				" 221B Baker Street ",
				" Floor 2 ",
				" London ",
				" Greater London ",
				" NW1 ",
				" gb ");

		assertThat(address.addressLine1()).isEqualTo("221B Baker Street");
		assertThat(address.addressLine2Value()).contains("Floor 2");
		assertThat(address.city()).isEqualTo("London");
		assertThat(address.stateOrRegion()).isEqualTo("Greater London");
		assertThat(address.postalCode()).isEqualTo("NW1");
		assertThat(address.countryCode()).isEqualTo("GB");
	}

	@Test
	void treatsBlankAddressLine2AsAbsent() {
		Address address = validAddress(" ");

		assertThat(address.addressLine2()).isNull();
		assertThat(address.addressLine2Value()).isEmpty();
	}

	@Test
	void rejectsNullAndBlankRequiredFields() {
		assertThatThrownBy(() -> new Address(null, null, "City", "State", "12345", "IN"))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, " ", "State", "12345", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", " ", "12345", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "State", " ", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void rejectsInvalidCountryCodes() {
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "State", "12345", "I"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "State", "12345", "IND"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "State", "12345", "12"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void enforcesDocumentedMaximumLengths() {
		assertThat(new Address("A".repeat(200), "B".repeat(200), "C".repeat(100), "S".repeat(100), "P".repeat(20), "IN"))
				.isNotNull();
		assertThatThrownBy(() -> new Address("A".repeat(201), null, "City", "State", "12345", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "C".repeat(101), "State", "12345", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "S".repeat(101), "12345", "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new Address("Line 1", null, "City", "State", "P".repeat(21), "IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(validAddress(null)).isEqualTo(validAddress(null));
	}

	private static Address validAddress(String addressLine2) {
		return new Address("Line 1", addressLine2, "City", "State", "12345", "IN");
	}
}
