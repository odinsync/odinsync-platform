package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

	@Test
	void createsValidEmailAndNormalizesLowercase() {
		EmailAddress email = new EmailAddress(" Owner@Example.COM ");

		assertThat(email.value()).isEqualTo("owner@example.com");
	}

	@Test
	void rejectsInvalidEmailValues() {
		assertThatThrownBy(() -> new EmailAddress(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EmailAddress(" "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new EmailAddress("@example.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new EmailAddress("owner@"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new EmailAddress("owner @example.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void enforcesMaximumLength() {
		String maxEmail = "a".repeat(64) + "@" + "b".repeat(186) + ".co";
		assertThat(maxEmail).hasSize(254);
		assertThat(new EmailAddress(maxEmail).value()).isEqualTo(maxEmail);
		assertThatThrownBy(() -> new EmailAddress(maxEmail + "m"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new EmailAddress("owner@example.com")).isEqualTo(new EmailAddress("OWNER@EXAMPLE.COM"));
	}
}
