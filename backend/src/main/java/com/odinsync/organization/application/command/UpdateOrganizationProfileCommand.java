package com.odinsync.organization.application.command;

import java.util.Objects;

import com.odinsync.organization.domain.valueobject.Address;
import com.odinsync.organization.domain.valueobject.OrganizationContact;
import com.odinsync.organization.domain.valueobject.OrganizationName;
import com.odinsync.organization.domain.valueobject.TaxRegistrationNumber;

public record UpdateOrganizationProfileCommand(
		OrganizationName name,
		TaxRegistrationNumber taxRegistrationNumber,
		Address address,
		OrganizationContact contact) {

	public UpdateOrganizationProfileCommand {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(taxRegistrationNumber, "taxRegistrationNumber must not be null");
		Objects.requireNonNull(address, "address must not be null");
		Objects.requireNonNull(contact, "contact must not be null");
	}
}
