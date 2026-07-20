package com.odinsync.organization.domain.valueobject;

import java.util.Objects;

public record OrganizationContact(
		EmailAddress email,
		PhoneNumber phone,
		Website website
) {
	public OrganizationContact {
		email = Objects.requireNonNull(email, "email must not be null");
		phone = Objects.requireNonNull(phone, "phone must not be null");
		website = Objects.requireNonNull(website, "website must not be null");
	}
}
