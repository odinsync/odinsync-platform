package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class CurrencyCodeTest {

	@Test
	void createsCanonicalCurrencyCode() {
		assertThat(new CurrencyCode("INR").value()).isEqualTo("INR");
		assertThat(new CurrencyCode(" usd ").value()).isEqualTo("USD");
	}

	@Test
	void rejectsInvalidCurrencyCodes() {
		assertThatThrownBy(() -> new CurrencyCode(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new CurrencyCode(" "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new CurrencyCode("ZZZ"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new CurrencyCode("USDD"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new CurrencyCode("inr")).isEqualTo(new CurrencyCode("INR"));
	}
}
