package com.odinsync.organization.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.event.OrganizationCreated;
import com.odinsync.organization.domain.event.OrganizationDomainEvent;
import com.odinsync.organization.domain.event.OrganizationProfileUpdated;
import com.odinsync.organization.domain.event.OrganizationSettingsUpdated;
import com.odinsync.organization.domain.event.OrganizationStatusChanged;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import com.odinsync.organization.domain.exception.OrganizationStateConflictException;
import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.AuditMetadata;
import com.odinsync.organization.domain.valueobject.OrganizationContact;
import com.odinsync.organization.domain.valueobject.OrganizationName;
import com.odinsync.organization.domain.valueobject.OrganizationSettings;
import com.odinsync.organization.domain.valueobject.TaxRegistrationNumber;

public class Organization {

	private final UUID id;
	private final UUID tenantId;
	private final List<OrganizationDomainEvent> domainEvents;
	private OrganizationName name;
	private TaxRegistrationNumber taxRegistrationNumber;
	private Address address;
	private OrganizationContact contact;
	private OrganizationSettings settings;
	private OrganizationStatus status;
	private AuditMetadata auditMetadata;

	private Organization(
			UUID id,
			UUID tenantId,
			OrganizationName name,
			TaxRegistrationNumber taxRegistrationNumber,
			Address address,
			OrganizationContact contact,
			OrganizationSettings settings,
			OrganizationStatus status,
			AuditMetadata auditMetadata) {
		this.id = Objects.requireNonNull(id, "organizationId must not be null");
		this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.taxRegistrationNumber = Objects.requireNonNull(
				taxRegistrationNumber,
				"taxRegistrationNumber must not be null");
		this.address = Objects.requireNonNull(address, "address must not be null");
		this.contact = Objects.requireNonNull(contact, "contact must not be null");
		this.settings = Objects.requireNonNull(settings, "settings must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.auditMetadata = Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
		this.domainEvents = new ArrayList<>();
	}

	public static Organization create(
			UUID organizationId,
			UUID tenantId,
			OrganizationName name,
			TaxRegistrationNumber taxRegistrationNumber,
			Address address,
			OrganizationContact contact,
			OrganizationSettings settings,
			Instant createdAt,
			UUID createdBy) {
		AuditMetadata auditMetadata = new AuditMetadata(createdAt, createdBy, createdAt, createdBy);
		Organization organization = new Organization(
				organizationId,
				tenantId,
				name,
				taxRegistrationNumber,
				address,
				contact,
				settings,
				OrganizationStatus.ACTIVE,
				auditMetadata);
		organization.record(new OrganizationCreated(
				UUID.randomUUID(),
				organization.id,
				organization.tenantId,
				createdAt,
				organization.status));
		return organization;
	}

	public static Organization reconstitute(
			UUID organizationId,
			UUID tenantId,
			OrganizationName name,
			TaxRegistrationNumber taxRegistrationNumber,
			Address address,
			OrganizationContact contact,
			OrganizationSettings settings,
			OrganizationStatus status,
			AuditMetadata auditMetadata) {
		return new Organization(
				organizationId,
				tenantId,
				name,
				taxRegistrationNumber,
				address,
				contact,
				settings,
				status,
				auditMetadata);
	}

	public void updateProfile(
			OrganizationName name,
			TaxRegistrationNumber taxRegistrationNumber,
			Address address,
			OrganizationContact contact,
			Instant updatedAt,
			UUID updatedBy) {
		OrganizationName newName = Objects.requireNonNull(name, "name must not be null");
		TaxRegistrationNumber newTaxRegistrationNumber = Objects.requireNonNull(
				taxRegistrationNumber,
				"taxRegistrationNumber must not be null");
		Address newAddress = Objects.requireNonNull(address, "address must not be null");
		OrganizationContact newContact = Objects.requireNonNull(contact, "contact must not be null");
		validateMutable();
		validateMutationMetadata(updatedAt, updatedBy);

		if (this.name.equals(newName)
				&& this.taxRegistrationNumber.equals(newTaxRegistrationNumber)
				&& this.address.equals(newAddress)
				&& this.contact.equals(newContact)) {
			return;
		}

		this.name = newName;
		this.taxRegistrationNumber = newTaxRegistrationNumber;
		this.address = newAddress;
		this.contact = newContact;
		this.auditMetadata = auditMetadata.updated(updatedAt, updatedBy);
		record(new OrganizationProfileUpdated(UUID.randomUUID(), id, tenantId, updatedAt, updatedBy));
	}

	public void updateSettings(
			OrganizationSettings settings,
			Instant updatedAt,
			UUID updatedBy) {
		OrganizationSettings newSettings = Objects.requireNonNull(settings, "settings must not be null");
		validateMutable();
		validateMutationMetadata(updatedAt, updatedBy);

		if (this.settings.equals(newSettings)) {
			return;
		}

		this.settings = newSettings;
		this.auditMetadata = auditMetadata.updated(updatedAt, updatedBy);
		record(new OrganizationSettingsUpdated(UUID.randomUUID(), id, tenantId, updatedAt, updatedBy));
	}

	public void activate(Instant changedAt, UUID changedBy) {
		if (status == OrganizationStatus.ACTIVE) {
			validateMutationMetadata(changedAt, changedBy);
			return;
		}
		changeStatus(OrganizationStatus.ACTIVE, changedAt, changedBy);
	}

	public void suspend(Instant changedAt, UUID changedBy) {
		if (status == OrganizationStatus.SUSPENDED) {
			validateMutationMetadata(changedAt, changedBy);
			return;
		}
		changeStatus(OrganizationStatus.SUSPENDED, changedAt, changedBy);
	}

	public void archive(Instant changedAt, UUID changedBy) {
		if (status == OrganizationStatus.ARCHIVED) {
			validateMutationMetadata(changedAt, changedBy);
			return;
		}
		changeStatus(OrganizationStatus.ARCHIVED, changedAt, changedBy);
	}

	public List<OrganizationDomainEvent> pullDomainEvents() {
		List<OrganizationDomainEvent> pendingEvents = List.copyOf(domainEvents);
		domainEvents.clear();
		return pendingEvents;
	}

	public UUID id() {
		return id;
	}

	public UUID tenantId() {
		return tenantId;
	}

	public OrganizationName name() {
		return name;
	}

	public TaxRegistrationNumber taxRegistrationNumber() {
		return taxRegistrationNumber;
	}

	public Address address() {
		return address;
	}

	public OrganizationContact contact() {
		return contact;
	}

	public OrganizationSettings settings() {
		return settings;
	}

	public OrganizationStatus status() {
		return status;
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	private void changeStatus(OrganizationStatus newStatus, Instant changedAt, UUID changedBy) {
		Objects.requireNonNull(newStatus, "newStatus must not be null");
		validateMutationMetadata(changedAt, changedBy);
		if (status == OrganizationStatus.ARCHIVED) {
			throw new ArchivedOrganizationModificationException();
		}
		if (newStatus == OrganizationStatus.SUSPENDED && status != OrganizationStatus.ACTIVE) {
			throw invalidTransition(newStatus);
		}
		OrganizationStatus previousStatus = status;
		this.status = newStatus;
		this.auditMetadata = auditMetadata.updated(changedAt, changedBy);
		record(new OrganizationStatusChanged(
				UUID.randomUUID(),
				id,
				tenantId,
				changedAt,
				changedBy,
				previousStatus,
				newStatus));
	}

	private void validateMutable() {
		if (status == OrganizationStatus.ARCHIVED) {
			throw new ArchivedOrganizationModificationException();
		}
	}

	private void validateMutationMetadata(Instant changedAt, UUID changedBy) {
		Objects.requireNonNull(changedAt, "changedAt must not be null");
		Objects.requireNonNull(changedBy, "changedBy must not be null");
		if (changedAt.isBefore(auditMetadata.createdAt())) {
			throw new InvalidOrganizationValueException("Mutation timestamp cannot be earlier than creation timestamp");
		}
		if (changedAt.isBefore(auditMetadata.updatedAt())) {
			throw new InvalidOrganizationValueException("Mutation timestamp cannot be earlier than the last update timestamp");
		}
	}

	private OrganizationStateConflictException invalidTransition(OrganizationStatus newStatus) {
		return new OrganizationStateConflictException(
				"Organization cannot transition from " + status + " to " + newStatus);
	}

	private void record(OrganizationDomainEvent event) {
		domainEvents.add(event);
	}
}
