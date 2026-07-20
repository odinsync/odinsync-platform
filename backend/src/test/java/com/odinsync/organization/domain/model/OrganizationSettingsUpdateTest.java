package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.LATER_AT;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.SECOND_ACTOR_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.event.OrganizationSettingsUpdated;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class OrganizationSettingsUpdateTest {

	@Test
	void updatesSettingsAndAuditMetadata() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();

		organization.updateSettings(OrganizationTestFixtures.changedSettings(), UPDATED_AT, SECOND_ACTOR_ID);

		assertThat(organization.settings()).isEqualTo(OrganizationTestFixtures.changedSettings());
		assertThat(organization.auditMetadata().createdAt()).isEqualTo(OrganizationTestFixtures.CREATED_AT);
		assertThat(organization.auditMetadata().createdBy()).isEqualTo(OrganizationTestFixtures.ACTOR_ID);
		assertThat(organization.auditMetadata().updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(organization.auditMetadata().updatedBy()).isEqualTo(SECOND_ACTOR_ID);
		assertThat(organization.pullDomainEvents())
				.singleElement()
				.isInstanceOfSatisfying(OrganizationSettingsUpdated.class, event -> {
					assertThat(event.occurredAt()).isEqualTo(UPDATED_AT);
					assertThat(event.changedBy()).isEqualTo(SECOND_ACTOR_ID);
				});
	}

	@Test
	void noOpSettingsUpdateDoesNotChangeAuditOrEmitEvent() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();
		var auditMetadata = organization.auditMetadata();

		organization.updateSettings(organization.settings(), UPDATED_AT, SECOND_ACTOR_ID);

		assertThat(organization.auditMetadata()).isEqualTo(auditMetadata);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void suspendedOrganizationCanUpdateSettings() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);

		organization.updateSettings(OrganizationTestFixtures.changedSettings(), LATER_AT, SECOND_ACTOR_ID);

		assertThat(organization.settings()).isEqualTo(OrganizationTestFixtures.changedSettings());
		assertThat(organization.status()).isEqualTo(OrganizationStatus.SUSPENDED);
	}

	@Test
	void archivedOrganizationRejectsSettingsUpdate() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ARCHIVED);

		assertThatThrownBy(() -> organization.updateSettings(OrganizationTestFixtures.changedSettings(), LATER_AT, SECOND_ACTOR_ID))
				.isInstanceOf(ArchivedOrganizationModificationException.class);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void invalidSettingsUpdateLeavesStateUnchanged() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ACTIVE);
		var originalSettings = organization.settings();
		var originalAudit = organization.auditMetadata();

		assertThatThrownBy(() -> organization.updateSettings(
				OrganizationTestFixtures.changedSettings(),
				UPDATED_AT.minusSeconds(1),
				SECOND_ACTOR_ID))
				.isInstanceOf(InvalidOrganizationValueException.class);

		assertThat(organization.settings()).isEqualTo(originalSettings);
		assertThat(organization.auditMetadata()).isEqualTo(originalAudit);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void rejectsNullSettingsUpdateArguments() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();
		assertThatThrownBy(() -> organization.updateSettings(null, UPDATED_AT, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateSettings(OrganizationTestFixtures.changedSettings(), null, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateSettings(OrganizationTestFixtures.changedSettings(), UPDATED_AT, null))
				.isInstanceOf(NullPointerException.class);
	}
}
