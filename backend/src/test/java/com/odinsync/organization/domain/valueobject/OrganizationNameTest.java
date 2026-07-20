package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class OrganizationNameTest {

	@Test
	void createsValidNameAndTrimsValues() {
		OrganizationName name = new OrganizationName(" Odin Retail Legal ", " Odin Retail ");

		assertThat(name.legalName()).isEqualTo("Odin Retail Legal");
		assertThat(name.displayName()).isEqualTo("Odin Retail");
	}

	@Test
	void acceptsMaximumBoundaryLengths() {
		OrganizationName name = new OrganizationName("L".repeat(200), "D".repeat(120));

		assertThat(name.legalName()).hasSize(200);
		assertThat(name.displayName()).hasSize(120);
	}

	@Test
	void rejectsInvalidLegalName() {
		assertThatThrownBy(() -> new OrganizationName(null, "Odin"))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationName(" ", "Odin"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationName("L".repeat(201), "Odin"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void rejectsInvalidDisplayName() {
		assertThatThrownBy(() -> new OrganizationName("Odin Legal", null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationName("Odin Legal", " "))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> new OrganizationName("Odin Legal", "D".repeat(121)))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEquality() {
		OrganizationName first = new OrganizationName("Odin Legal", "Odin");
		OrganizationName second = new OrganizationName("Odin Legal", "Odin");

		assertThat(first).isEqualTo(second);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}
}
