package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class OrganizationLocaleTest {

	@Test
	void createsCanonicalBcp47LocaleTag() {
		assertThat(new OrganizationLocale("en-IN").value()).isEqualTo("en-IN");
		assertThat(new OrganizationLocale(" en-in ").value()).isEqualTo("en-IN");
		assertThat(new OrganizationLocale("fr-FR").value()).isEqualTo("fr-FR");
	}

	@Test
	void rejectsInvalidLocaleValues() {
		assertThatThrownBy(() -> new OrganizationLocale(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationLocale(" "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationLocale("en--IN"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationLocale("und"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new OrganizationLocale("en-in")).isEqualTo(new OrganizationLocale("en-IN"));
	}
}
