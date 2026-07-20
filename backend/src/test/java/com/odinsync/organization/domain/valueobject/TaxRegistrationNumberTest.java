package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class TaxRegistrationNumberTest {

	@Test
	void createsValidValueAndTrimsWhitespace() {
		TaxRegistrationNumber number = TaxRegistrationNumber.of(" TAX-123 ");

		assertThat(number.value()).contains("TAX-123");
	}

	@Test
	void normalizesNullAndBlankToAbsent() {
		assertThat(TaxRegistrationNumber.of(null).value()).isEmpty();
		assertThat(TaxRegistrationNumber.of(" ").value()).isEmpty();
		assertThat(TaxRegistrationNumber.empty().value()).isEmpty();
	}

	@Test
	void acceptsMaximumBoundaryAndRejectsAboveMaximum() {
		assertThat(TaxRegistrationNumber.of("T".repeat(50)).value()).contains("T".repeat(50));
		assertThatThrownBy(() -> TaxRegistrationNumber.of("T".repeat(51)))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEqualityForPresentAndAbsentValues() {
		assertThat(TaxRegistrationNumber.of("ABC")).isEqualTo(TaxRegistrationNumber.of("ABC"));
		assertThat(TaxRegistrationNumber.of(null)).isEqualTo(TaxRegistrationNumber.of(" "));
	}
}
