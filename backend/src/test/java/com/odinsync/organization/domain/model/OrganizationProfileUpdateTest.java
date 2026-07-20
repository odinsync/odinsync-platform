package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.LATER_AT;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.SECOND_ACTOR_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.event.OrganizationProfileUpdated;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class OrganizationProfileUpdateTest {

	@Test
	void updatesAllProfileFieldsAndAuditMetadata() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();

		organization.updateProfile(
				OrganizationTestFixtures.changedName(),
				OrganizationTestFixtures.changedTaxRegistrationNumber(),
				OrganizationTestFixtures.changedAddress(),
				OrganizationTestFixtures.changedContact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);

		assertThat(organization.name()).isEqualTo(OrganizationTestFixtures.changedName());
		assertThat(organization.taxRegistrationNumber()).isEqualTo(OrganizationTestFixtures.changedTaxRegistrationNumber());
		assertThat(organization.address()).isEqualTo(OrganizationTestFixtures.changedAddress());
		assertThat(organization.contact()).isEqualTo(OrganizationTestFixtures.changedContact());
		assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organization.settings()).isEqualTo(OrganizationTestFixtures.settings());
		assertThat(organization.auditMetadata().createdAt()).isEqualTo(OrganizationTestFixtures.CREATED_AT);
		assertThat(organization.auditMetadata().createdBy()).isEqualTo(OrganizationTestFixtures.ACTOR_ID);
		assertThat(organization.auditMetadata().updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(organization.auditMetadata().updatedBy()).isEqualTo(SECOND_ACTOR_ID);
		assertThat(organization.pullDomainEvents())
				.singleElement()
				.isInstanceOfSatisfying(OrganizationProfileUpdated.class, event -> {
					assertThat(event.occurredAt()).isEqualTo(UPDATED_AT);
					assertThat(event.changedBy()).isEqualTo(SECOND_ACTOR_ID);
				});
	}

	@Test
	void updatesIndividualProfileFields() {
		Organization nameOnly = OrganizationTestFixtures.newOrganization();
		nameOnly.pullDomainEvents();
		nameOnly.updateProfile(
				OrganizationTestFixtures.changedName(),
				nameOnly.taxRegistrationNumber(),
				nameOnly.address(),
				nameOnly.contact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);
		assertThat(nameOnly.name()).isEqualTo(OrganizationTestFixtures.changedName());

		Organization taxOnly = OrganizationTestFixtures.newOrganization();
		taxOnly.pullDomainEvents();
		taxOnly.updateProfile(
				taxOnly.name(),
				OrganizationTestFixtures.changedTaxRegistrationNumber(),
				taxOnly.address(),
				taxOnly.contact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);
		assertThat(taxOnly.taxRegistrationNumber()).isEqualTo(OrganizationTestFixtures.changedTaxRegistrationNumber());

		Organization addressOnly = OrganizationTestFixtures.newOrganization();
		addressOnly.pullDomainEvents();
		addressOnly.updateProfile(
				addressOnly.name(),
				addressOnly.taxRegistrationNumber(),
				OrganizationTestFixtures.changedAddress(),
				addressOnly.contact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);
		assertThat(addressOnly.address()).isEqualTo(OrganizationTestFixtures.changedAddress());

		Organization contactOnly = OrganizationTestFixtures.newOrganization();
		contactOnly.pullDomainEvents();
		contactOnly.updateProfile(
				contactOnly.name(),
				contactOnly.taxRegistrationNumber(),
				contactOnly.address(),
				OrganizationTestFixtures.changedContact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);
		assertThat(contactOnly.contact()).isEqualTo(OrganizationTestFixtures.changedContact());
	}

	@Test
	void noOpProfileUpdateDoesNotChangeAuditOrEmitEvent() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();
		var auditMetadata = organization.auditMetadata();

		organization.updateProfile(
				organization.name(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);

		assertThat(organization.auditMetadata()).isEqualTo(auditMetadata);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void suspendedOrganizationCanUpdateProfile() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);

		organization.updateProfile(
				OrganizationTestFixtures.changedName(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				LATER_AT,
				SECOND_ACTOR_ID);

		assertThat(organization.name()).isEqualTo(OrganizationTestFixtures.changedName());
		assertThat(organization.status()).isEqualTo(OrganizationStatus.SUSPENDED);
	}

	@Test
	void archivedOrganizationRejectsProfileUpdate() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ARCHIVED);

		assertThatThrownBy(() -> organization.updateProfile(
				OrganizationTestFixtures.changedName(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				LATER_AT,
				SECOND_ACTOR_ID))
				.isInstanceOf(ArchivedOrganizationModificationException.class);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void invalidProfileUpdateLeavesStateUnchanged() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ACTIVE);
		var originalName = organization.name();
		var originalAudit = organization.auditMetadata();

		assertThatThrownBy(() -> organization.updateProfile(
				OrganizationTestFixtures.changedName(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				UPDATED_AT.minusSeconds(1),
				SECOND_ACTOR_ID))
				.isInstanceOf(InvalidOrganizationValueException.class);

		assertThat(organization.name()).isEqualTo(originalName);
		assertThat(organization.auditMetadata()).isEqualTo(originalAudit);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void rejectsNullProfileUpdateArguments() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();
		assertThatThrownBy(() -> organization.updateProfile(null, organization.taxRegistrationNumber(), organization.address(), organization.contact(), UPDATED_AT, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateProfile(organization.name(), null, organization.address(), organization.contact(), UPDATED_AT, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateProfile(organization.name(), organization.taxRegistrationNumber(), null, organization.contact(), UPDATED_AT, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateProfile(organization.name(), organization.taxRegistrationNumber(), organization.address(), null, UPDATED_AT, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateProfile(organization.name(), organization.taxRegistrationNumber(), organization.address(), organization.contact(), null, SECOND_ACTOR_ID))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> organization.updateProfile(organization.name(), organization.taxRegistrationNumber(), organization.address(), organization.contact(), UPDATED_AT, null))
				.isInstanceOf(NullPointerException.class);
	}
}
