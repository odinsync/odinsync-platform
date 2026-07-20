package com.odinsync.organization.application.result;

import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.model.OrganizationStatus;
import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.AuditMetadata;
import com.odinsync.organization.domain.valueobject.OrganizationContact;
import com.odinsync.organization.domain.valueobject.OrganizationName;
import com.odinsync.organization.domain.valueobject.TaxRegistrationNumber;

public record OrganizationProfileResult(
		UUID organizationId,
		UUID tenantId,
		OrganizationName name,
		TaxRegistrationNumber taxRegistrationNumber,
		Address address,
		OrganizationContact contact,
		OrganizationStatus status,
		AuditMetadata auditMetadata) {

	public OrganizationProfileResult {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(taxRegistrationNumber, "taxRegistrationNumber must not be null");
		Objects.requireNonNull(address, "address must not be null");
		Objects.requireNonNull(contact, "contact must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}
}
