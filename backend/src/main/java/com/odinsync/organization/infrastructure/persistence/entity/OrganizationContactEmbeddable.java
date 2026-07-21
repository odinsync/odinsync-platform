package com.odinsync.organization.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class OrganizationContactEmbeddable {

	@Column(name = "contact_email", length = 254, nullable = false)
	private String email;

	@Column(name = "contact_phone", length = 30, nullable = false)
	private String phone;

	@Column(name = "contact_website", length = 500)
	private String website;
}
