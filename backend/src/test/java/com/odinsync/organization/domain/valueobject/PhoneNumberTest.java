package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class PhoneNumberTest {

	@Test
	void acceptsLocalInternationalAndSeparatedNumbers() {
		assertThat(new PhoneNumber(" 080-1234 5678 ").value()).isEqualTo("080-1234 5678");
		assertThat(new PhoneNumber("+91 (80) 1234-5678").value()).isEqualTo("+91 (80) 1234-5678");
	}

	@Test
	void rejectsInvalidNumbers() {
		assertThatThrownBy(() -> new PhoneNumber(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new PhoneNumber(" "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new PhoneNumber("1800-CALL-NOW"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new PhoneNumber("+91 80 #1234"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new PhoneNumber("+ () -"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void enforcesMaximumLength() {
		assertThat(new PhoneNumber("1".repeat(30)).value()).hasSize(30);
		assertThatThrownBy(() -> new PhoneNumber("1".repeat(31)))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new PhoneNumber("+91 80 1234")).isEqualTo(new PhoneNumber("+91 80 1234"));
	}
}
