package com.odinsync.organization.infrastructure.persistence.entity;

import java.util.UUID;

import com.odinsync.organization.domain.model.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "OrganizationAggregateJpaEntity")
@Table(name = "organizations")
public class OrganizationJpaEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Column(name = "legal_name", length = 200, nullable = false)
	private String legalName;

	@Column(name = "display_name", length = 120, nullable = false)
	private String displayName;

	@Column(name = "tax_registration_number", length = 50)
	private String taxRegistrationNumber;

	@Embedded
	private OrganizationAddressEmbeddable address;

	@Embedded
	private OrganizationContactEmbeddable contact;

	@Embedded
	private OrganizationSettingsEmbeddable settings;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	private OrganizationStatus status;

	@Embedded
	private OrganizationAuditEmbeddable audit;

	@Version
	@Column(name = "version", nullable = false)
	private long version;
}
