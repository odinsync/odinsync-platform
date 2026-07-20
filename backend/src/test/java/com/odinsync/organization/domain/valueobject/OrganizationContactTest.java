package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrganizationContactTest {

	@Test
	void createsValidContactWithAndWithoutWebsite() {
		OrganizationContact withWebsite = new OrganizationContact(
				new EmailAddress("owner@example.com"),
				new PhoneNumber("+91 80 1234"),
				Website.of("https://example.com"));
		OrganizationContact withoutWebsite = new OrganizationContact(
				new EmailAddress("owner@example.com"),
				new PhoneNumber("+91 80 1234"),
				Website.empty());

		assertThat(withWebsite.website().value()).contains("https://example.com");
		assertThat(withoutWebsite.website().value()).isEmpty();
	}

	@Test
	void rejectsNullRequiredComponents() {
		assertThatThrownBy(() -> new OrganizationContact(null, new PhoneNumber("123"), Website.empty()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationContact(new EmailAddress("owner@example.com"), null, Website.empty()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new OrganizationContact(new EmailAddress("owner@example.com"), new PhoneNumber("123"), null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void usesValueBasedEquality() {
		OrganizationContact first = new OrganizationContact(
				new EmailAddress("owner@example.com"),
				new PhoneNumber("123"),
				Website.empty());

		assertThat(first).isEqualTo(new OrganizationContact(
				new EmailAddress("OWNER@EXAMPLE.COM"),
				new PhoneNumber("123"),
				Website.of(null)));
	}
}
