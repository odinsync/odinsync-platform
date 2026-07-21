package com.odinsync.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

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
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationAddressEmbeddable;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationAuditEmbeddable;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationContactEmbeddable;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationSettingsEmbeddable;

public final class OrganizationPersistenceTestFixtures {

	public static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	public static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	public static final UUID CREATED_BY = UUID.fromString("33333333-3333-3333-3333-333333333333");
	public static final UUID UPDATED_BY = UUID.fromString("44444444-4444-4444-4444-444444444444");
	public static final Instant CREATED_AT = Instant.parse("2026-07-20T10:00:00Z");
	public static final Instant UPDATED_AT = Instant.parse("2026-07-21T10:00:00Z");

	private OrganizationPersistenceTestFixtures() {
	}

	public static Organization organization() {
		return Organization.reconstitute(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				TaxRegistrationNumber.of("TAX-123"),
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
				TaxRegistrationNumber.empty(),
				address(),
				contact(),
				settings(),
				OrganizationStatus.ARCHIVED,
				auditMetadata());
	}

	public static Organization createdOrganizationWithPendingEvent() {
		return Organization.create(
				ORGANIZATION_ID,
				TENANT_ID,
				name(),
				TaxRegistrationNumber.of("TAX-123"),
				address(),
				contact(),
				settings(),
				CREATED_AT,
				CREATED_BY);
	}

	public static OrganizationJpaEntity entity() {
		OrganizationJpaEntity entity = new OrganizationJpaEntity();
		entity.setId(ORGANIZATION_ID);
		entity.setTenantId(TENANT_ID);
		entity.setLegalName("Odin Retail Private Limited");
		entity.setDisplayName("Odin Retail");
		entity.setTaxRegistrationNumber("TAX-123");
		entity.setAddress(addressEmbeddable());
		entity.setContact(contactEmbeddable());
		entity.setSettings(settingsEmbeddable());
		entity.setStatus(OrganizationStatus.ACTIVE);
		entity.setAudit(auditEmbeddable());
		entity.setVersion(7L);
		return entity;
	}

	public static OrganizationJpaEntity archivedEntity() {
		OrganizationJpaEntity entity = entity();
		entity.setStatus(OrganizationStatus.ARCHIVED);
		entity.setTaxRegistrationNumber(null);
		return entity;
	}

	public static OrganizationJpaEntity suspendedEntity() {
		OrganizationJpaEntity entity = entity();
		entity.setStatus(OrganizationStatus.SUSPENDED);
		return entity;
	}

	public static OrganizationJpaEntity updateTargetEntity() {
		OrganizationJpaEntity entity = entity();
		entity.setLegalName("Old Legal Name");
		entity.setDisplayName("Old Display");
		entity.getAudit().setCreatedAt(CREATED_AT.minusSeconds(3600));
		entity.getAudit().setCreatedBy(UUID.fromString("55555555-5555-5555-5555-555555555555"));
		entity.getAudit().setUpdatedAt(CREATED_AT.minusSeconds(1800));
		entity.getAudit().setUpdatedBy(UUID.fromString("66666666-6666-6666-6666-666666666666"));
		entity.setVersion(42L);
		return entity;
	}

	private static OrganizationName name() {
		return new OrganizationName("Odin Retail Private Limited", "Odin Retail");
	}

	private static Address address() {
		return new Address("Line 1", "Line 2", "Bengaluru", "Karnataka", "560001", "IN");
	}

	private static OrganizationContact contact() {
		return new OrganizationContact(
				new EmailAddress("OWNER@ODINSYNC.COM"),
				new PhoneNumber("+91 80 1234 5678"),
				Website.of("https://odinsync.com"));
	}

	private static OrganizationSettings settings() {
		return new OrganizationSettings(
				new CurrencyCode("INR"),
				new OrganizationTimeZone("Asia/Kolkata"),
				new OrganizationLocale("en-IN"),
				DateFormat.DD_MM_YYYY,
				TimeFormat.TWENTY_FOUR_HOUR,
				WeekStart.MONDAY);
	}

	private static AuditMetadata auditMetadata() {
		return new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY);
	}

	private static OrganizationAddressEmbeddable addressEmbeddable() {
		OrganizationAddressEmbeddable address = new OrganizationAddressEmbeddable();
		address.setAddressLine1("Line 1");
		address.setAddressLine2("Line 2");
		address.setCity("Bengaluru");
		address.setStateOrRegion("Karnataka");
		address.setPostalCode("560001");
		address.setCountryCode("IN");
		return address;
	}

	private static OrganizationContactEmbeddable contactEmbeddable() {
		OrganizationContactEmbeddable contact = new OrganizationContactEmbeddable();
		contact.setEmail("owner@odinsync.com");
		contact.setPhone("+91 80 1234 5678");
		contact.setWebsite("https://odinsync.com");
		return contact;
	}

	private static OrganizationSettingsEmbeddable settingsEmbeddable() {
		OrganizationSettingsEmbeddable settings = new OrganizationSettingsEmbeddable();
		settings.setCurrencyCode("INR");
		settings.setTimeZone("Asia/Kolkata");
		settings.setLocale("en-IN");
		settings.setDateFormat(DateFormat.DD_MM_YYYY);
		settings.setTimeFormat(TimeFormat.TWENTY_FOUR_HOUR);
		settings.setWeekStart(WeekStart.MONDAY);
		return settings;
	}

	private static OrganizationAuditEmbeddable auditEmbeddable() {
		OrganizationAuditEmbeddable audit = new OrganizationAuditEmbeddable();
		audit.setCreatedAt(CREATED_AT);
		audit.setCreatedBy(CREATED_BY);
		audit.setUpdatedAt(UPDATED_AT);
		audit.setUpdatedBy(UPDATED_BY);
		return audit;
	}
}
