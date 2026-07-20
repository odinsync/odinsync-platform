package com.odinsync.organization.domain.valueobject;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record AuditMetadata(
		Instant createdAt,
		UUID createdBy,
		Instant updatedAt,
		UUID updatedBy
) {
	public AuditMetadata {
		createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		updatedBy = Objects.requireNonNull(updatedBy, "updatedBy must not be null");
		if (updatedAt.isBefore(createdAt)) {
			throw new InvalidOrganizationValueException("updatedAt must not be earlier than createdAt");
		}
	}

	public AuditMetadata updated(Instant updatedAt, UUID updatedBy) {
		return new AuditMetadata(createdAt, createdBy, updatedAt, updatedBy);
	}
}
