package com.odinsync.organization.infrastructure.persistence.mapper;

import java.util.Objects;

import com.odinsync.organization.domain.model.Organization;
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
import org.springframework.stereotype.Component;

@Component
public class OrganizationPersistenceMapper {

	public OrganizationJpaEntity toNewEntity(Organization organization) {
		Objects.requireNonNull(organization, "organization must not be null");
		OrganizationJpaEntity entity = new OrganizationJpaEntity();
		entity.setId(organization.id());
		entity.setTenantId(organization.tenantId());
		copyMutableState(organization, entity, true);
		return entity;
	}

	public void updateEntity(Organization organization, OrganizationJpaEntity entity) {
		Objects.requireNonNull(organization, "organization must not be null");
		Objects.requireNonNull(entity, "entity must not be null");
		if (!organization.id().equals(entity.getId())) {
			throw new IllegalArgumentException("Organization ID does not match persistence entity ID");
		}
		if (!organization.tenantId().equals(entity.getTenantId())) {
			throw new IllegalArgumentException("Organization tenant ID does not match persistence entity tenant ID");
		}
		copyMutableState(organization, entity, false);
	}

	public Organization toDomain(OrganizationJpaEntity entity) {
		Objects.requireNonNull(entity, "entity must not be null");
		OrganizationName name = new OrganizationName(entity.getLegalName(), entity.getDisplayName());
		Address address = toAddress(entity.getAddress());
		OrganizationContact contact = toContact(entity.getContact());
		OrganizationSettings settings = toSettings(entity.getSettings());
		AuditMetadata auditMetadata = toAuditMetadata(entity.getAudit());
		return Organization.reconstitute(
				Objects.requireNonNull(entity.getId(), "entity id must not be null"),
				Objects.requireNonNull(entity.getTenantId(), "entity tenantId must not be null"),
				name,
				TaxRegistrationNumber.of(entity.getTaxRegistrationNumber()),
				address,
				contact,
				settings,
				Objects.requireNonNull(entity.getStatus(), "entity status must not be null"),
				auditMetadata);
	}

	private void copyMutableState(Organization organization, OrganizationJpaEntity entity, boolean includeCreationAudit) {
		entity.setLegalName(organization.name().legalName());
		entity.setDisplayName(organization.name().displayName());
		entity.setTaxRegistrationNumber(organization.taxRegistrationNumber().value().orElse(null));
		entity.setAddress(toAddressEmbeddable(organization.address()));
		entity.setContact(toContactEmbeddable(organization.contact()));
		entity.setSettings(toSettingsEmbeddable(organization.settings()));
		entity.setStatus(organization.status());
		entity.setAudit(toAuditEmbeddable(organization.auditMetadata(), entity.getAudit(), includeCreationAudit));
	}

	private OrganizationAddressEmbeddable toAddressEmbeddable(Address address) {
		OrganizationAddressEmbeddable embeddable = new OrganizationAddressEmbeddable();
		embeddable.setAddressLine1(address.addressLine1());
		embeddable.setAddressLine2(address.addressLine2Value().orElse(null));
		embeddable.setCity(address.city());
		embeddable.setStateOrRegion(address.stateOrRegion());
		embeddable.setPostalCode(address.postalCode());
		embeddable.setCountryCode(address.countryCode());
		return embeddable;
	}

	private OrganizationContactEmbeddable toContactEmbeddable(OrganizationContact contact) {
		OrganizationContactEmbeddable embeddable = new OrganizationContactEmbeddable();
		embeddable.setEmail(contact.email().value());
		embeddable.setPhone(contact.phone().value());
		embeddable.setWebsite(contact.website().value().orElse(null));
		return embeddable;
	}

	private OrganizationSettingsEmbeddable toSettingsEmbeddable(OrganizationSettings settings) {
		OrganizationSettingsEmbeddable embeddable = new OrganizationSettingsEmbeddable();
		embeddable.setCurrencyCode(settings.currencyCode().value());
		embeddable.setTimeZone(settings.timeZone().value());
		embeddable.setLocale(settings.locale().value());
		embeddable.setDateFormat(settings.dateFormat());
		embeddable.setTimeFormat(settings.timeFormat());
		embeddable.setWeekStart(settings.weekStart());
		return embeddable;
	}

	private OrganizationAuditEmbeddable toAuditEmbeddable(
			AuditMetadata auditMetadata,
			OrganizationAuditEmbeddable existingAudit,
			boolean includeCreationAudit) {
		OrganizationAuditEmbeddable embeddable = existingAudit == null
				? new OrganizationAuditEmbeddable()
				: existingAudit;
		if (includeCreationAudit) {
			embeddable.setCreatedAt(auditMetadata.createdAt());
			embeddable.setCreatedBy(auditMetadata.createdBy());
		}
		embeddable.setUpdatedAt(auditMetadata.updatedAt());
		embeddable.setUpdatedBy(auditMetadata.updatedBy());
		return embeddable;
	}

	private Address toAddress(OrganizationAddressEmbeddable embeddable) {
		Objects.requireNonNull(embeddable, "entity address must not be null");
		return new Address(
				embeddable.getAddressLine1(),
				embeddable.getAddressLine2(),
				embeddable.getCity(),
				embeddable.getStateOrRegion(),
				embeddable.getPostalCode(),
				embeddable.getCountryCode());
	}

	private OrganizationContact toContact(OrganizationContactEmbeddable embeddable) {
		Objects.requireNonNull(embeddable, "entity contact must not be null");
		return new OrganizationContact(
				new EmailAddress(embeddable.getEmail()),
				new PhoneNumber(embeddable.getPhone()),
				Website.of(embeddable.getWebsite()));
	}

	private OrganizationSettings toSettings(OrganizationSettingsEmbeddable embeddable) {
		Objects.requireNonNull(embeddable, "entity settings must not be null");
		return new OrganizationSettings(
				new CurrencyCode(embeddable.getCurrencyCode()),
				new OrganizationTimeZone(embeddable.getTimeZone()),
				new OrganizationLocale(embeddable.getLocale()),
				Objects.requireNonNull(embeddable.getDateFormat(), "entity dateFormat must not be null"),
				Objects.requireNonNull(embeddable.getTimeFormat(), "entity timeFormat must not be null"),
				Objects.requireNonNull(embeddable.getWeekStart(), "entity weekStart must not be null"));
	}

	private AuditMetadata toAuditMetadata(OrganizationAuditEmbeddable embeddable) {
		Objects.requireNonNull(embeddable, "entity audit must not be null");
		return new AuditMetadata(
				embeddable.getCreatedAt(),
				embeddable.getCreatedBy(),
				embeddable.getUpdatedAt(),
				embeddable.getUpdatedBy());
	}
}
