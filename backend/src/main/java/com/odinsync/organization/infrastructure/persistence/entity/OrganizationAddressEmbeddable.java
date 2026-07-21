package com.odinsync.organization.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class OrganizationAddressEmbeddable {

	@Column(name = "address_line1", length = 200, nullable = false)
	private String addressLine1;

	@Column(name = "address_line2", length = 200)
	private String addressLine2;

	@Column(name = "address_city", length = 100, nullable = false)
	private String city;

	@Column(name = "address_state_or_region", length = 100, nullable = false)
	private String stateOrRegion;

	@Column(name = "address_postal_code", length = 20, nullable = false)
	private String postalCode;

	@Column(name = "address_country_code", length = 2, nullable = false)
	private String countryCode;
}
