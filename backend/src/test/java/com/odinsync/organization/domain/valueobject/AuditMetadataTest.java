package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class AuditMetadataTest {

	private static final Instant CREATED_AT = Instant.parse("2026-07-20T00:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-07-20T01:00:00Z");
	private static final UUID CREATED_BY = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID UPDATED_BY = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void createsValidMetadata() {
		AuditMetadata metadata = new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY);

		assertThat(metadata.createdAt()).isEqualTo(CREATED_AT);
		assertThat(metadata.createdBy()).isEqualTo(CREATED_BY);
		assertThat(metadata.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(metadata.updatedBy()).isEqualTo(UPDATED_BY);
	}

	@Test
	void rejectsNullValues() {
		assertThatThrownBy(() -> new AuditMetadata(null, CREATED_BY, UPDATED_AT, UPDATED_BY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuditMetadata(CREATED_AT, null, UPDATED_AT, UPDATED_BY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuditMetadata(CREATED_AT, CREATED_BY, null, UPDATED_BY))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsUpdatedAtBeforeCreatedAt() {
		assertThatThrownBy(() -> new AuditMetadata(CREATED_AT, CREATED_BY, CREATED_AT.minusSeconds(1), UPDATED_BY))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void updateOperationPreservesCreationMetadata() {
		UUID nextUpdater = UUID.fromString("00000000-0000-0000-0000-000000000003");
		Instant nextUpdatedAt = Instant.parse("2026-07-20T02:00:00Z");

		AuditMetadata updated = new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
				.updated(nextUpdatedAt, nextUpdater);

		assertThat(updated.createdAt()).isEqualTo(CREATED_AT);
		assertThat(updated.createdBy()).isEqualTo(CREATED_BY);
		assertThat(updated.updatedAt()).isEqualTo(nextUpdatedAt);
		assertThat(updated.updatedBy()).isEqualTo(nextUpdater);
	}

	@Test
	void usesValueBasedEquality() {
		assertThat(new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY))
				.isEqualTo(new AuditMetadata(CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY));
	}
}
