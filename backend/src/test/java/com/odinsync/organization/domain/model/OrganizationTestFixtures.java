package com.odinsync.organization.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.CurrencyCode;
import com.odinsync.organization.domain.valueobject.EmailAddress;
import com.odinsync.organization.domain.valueobject.OrganizationContact;
import com.odinsync.organization.domain.valueobject.OrganizationLocale;
import com.odinsync.organization.domain.valueobject.OrganizationName;
import com.odinsync.organization.domain.valueobject.OrganizationSettings;
import com.odinsync.organization.domain.valueobject.OrganizationTimeZone;
import com.odinsync.organization.domain.valueobject.PhoneNumber;
import com.odinsync.organization.domain.valueobject.TaxRegistrationNumber;
import com.odinsync.organization.domain.valueobject.Website;

final class OrganizationTestFixtures {

	static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	static final UUID SECOND_ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	static final Instant CREATED_AT = Instant.parse("2026-07-20T10:00:00Z");
	static final Instant UPDATED_AT = Instant.parse("2026-07-20T11:00:00Z");
	static final Instant LATER_AT = Instant.parse("2026-07-20T12:00:00Z");

	private OrganizationTestFixtures() {
	}

	static Organization newOrganization() {
		return Organization.create(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				settings(),
				CREATED_AT,
				ACTOR_ID);
	}

	static Organization reconstituted(OrganizationStatus status) {
		return Organization.reconstitute(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				settings(),
				status,
				auditMetadata());
	}

	static com.odinsync.organization.domain.valueobject.AuditMetadata auditMetadata() {
		return new com.odinsync.organization.domain.valueobject.AuditMetadata(
				CREATED_AT,
				ACTOR_ID,
				UPDATED_AT,
				SECOND_ACTOR_ID);
	}

	static OrganizationName name() {
		return new OrganizationName("Odin Retail Private Limited", "Odin Retail");
	}

	static OrganizationName changedName() {
		return new OrganizationName("Odin Commerce Private Limited", "Odin Commerce");
	}

	static TaxRegistrationNumber taxRegistrationNumber() {
		return TaxRegistrationNumber.of("TAX-123");
	}

	static TaxRegistrationNumber changedTaxRegistrationNumber() {
		return TaxRegistrationNumber.of("TAX-456");
	}

	static Address address() {
		return new Address("Line 1", "Line 2", "Bengaluru", "Karnataka", "560001", "IN");
	}

	static Address changedAddress() {
		return new Address("Line 9", null, "Mumbai", "Maharashtra", "400001", "IN");
	}

	static OrganizationContact contact() {
		return new OrganizationContact(
				new EmailAddress("owner@odinsync.com"),
				new PhoneNumber("+91 80 1234 5678"),
				Website.of("https://odinsync.com"));
	}

	static OrganizationContact changedContact() {
		return new OrganizationContact(
				new EmailAddress("admin@odinsync.com"),
				new PhoneNumber("+91 22 1234 5678"),
				Website.empty());
	}

	static OrganizationSettings settings() {
		return new OrganizationSettings(
				new CurrencyCode("INR"),
				new OrganizationTimeZone("Asia/Kolkata"),
				new OrganizationLocale("en-IN"),
				DateFormat.DD_MM_YYYY,
				TimeFormat.TWENTY_FOUR_HOUR,
				WeekStart.MONDAY);
	}

	static OrganizationSettings changedSettings() {
		return new OrganizationSettings(
				new CurrencyCode("USD"),
				new OrganizationTimeZone("America/New_York"),
				new OrganizationLocale("en-US"),
				DateFormat.MM_DD_YYYY,
				TimeFormat.TWELVE_HOUR,
				WeekStart.SUNDAY);
	}
}
