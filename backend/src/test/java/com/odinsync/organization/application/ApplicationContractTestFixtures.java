package com.odinsync.organization.application;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.odinsync.organization.application.command.UpdateOrganizationProfileCommand;
import com.odinsync.organization.application.model.AuthenticatedActor;
import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.domain.model.OrganizationStatus;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;
import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.AuditMetadata;
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

public final class ApplicationContractTestFixtures {

	public static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	public static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	public static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	public static final Instant CREATED_AT = Instant.parse("2026-07-21T00:00:00Z");
	public static final Instant UPDATED_AT = Instant.parse("2026-07-21T01:00:00Z");

	private ApplicationContractTestFixtures() {
	}

	public static OrganizationName name() {
		return new OrganizationName("Odin Retail Private Limited", "Odin Retail");
	}

	public static OrganizationName changedName() {
		return new OrganizationName("Odin Commerce Private Limited", "Odin Commerce");
	}

	public static TaxRegistrationNumber taxRegistrationNumber() {
		return TaxRegistrationNumber.of("TAX-123");
	}

	public static TaxRegistrationNumber changedTaxRegistrationNumber() {
		return TaxRegistrationNumber.of("TAX-456");
	}

	public static Address address() {
		return new Address("Line 1", null, "Bengaluru", "Karnataka", "560001", "IN");
	}

	public static Address changedAddress() {
		return new Address("Line 2", "Suite 4", "Mumbai", "Maharashtra", "400001", "IN");
	}

	public static OrganizationContact contact() {
		return new OrganizationContact(
				new EmailAddress("owner@odinsync.com"),
				new PhoneNumber("+91 80 1234 5678"),
				Website.of("https://odinsync.com"));
	}

	public static OrganizationContact changedContact() {
		return new OrganizationContact(
				new EmailAddress("admin@odinsync.com"),
				new PhoneNumber("+91 22 1234 5678"),
				Website.of("https://admin.odinsync.com"));
	}

	public static OrganizationSettings settings() {
		return new OrganizationSettings(
				new CurrencyCode("INR"),
				new OrganizationTimeZone("Asia/Kolkata"),
				new OrganizationLocale("en-IN"),
				DateFormat.DD_MM_YYYY,
				TimeFormat.TWENTY_FOUR_HOUR,
				WeekStart.MONDAY);
	}

	public static AuditMetadata auditMetadata() {
		return new AuditMetadata(CREATED_AT, ACTOR_ID, CREATED_AT, ACTOR_ID);
	}

	public static AuthenticatedActor actor() {
		return new AuthenticatedActor(
				ACTOR_ID,
				TENANT_ID,
				Set.of("OWNER"),
				Set.of("organization:read", "organization:update"));
	}

	public static Organization organization() {
		return Organization.reconstitute(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				settings(),
				OrganizationStatus.ACTIVE,
				auditMetadata());
	}

	public static Organization archivedOrganization() {
		return Organization.reconstitute(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				taxRegistrationNumber(),
				address(),
				contact(),
				settings(),
				OrganizationStatus.ARCHIVED,
				auditMetadata());
	}

	public static UpdateOrganizationProfileCommand updateProfileCommand() {
		return new UpdateOrganizationProfileCommand(
				ORGANIZATION_ID,
				changedName(),
				changedTaxRegistrationNumber(),
				changedAddress(),
				changedContact());
	}
}
