package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.LATER_AT;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.SECOND_ACTOR_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.event.OrganizationStatusChanged;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import com.odinsync.organization.domain.exception.OrganizationStateConflictException;
import org.junit.jupiter.api.Test;

class OrganizationStatusTransitionTest {

	@Test
	void transitionsActiveToSuspended() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.pullDomainEvents();

		organization.suspend(UPDATED_AT, SECOND_ACTOR_ID);

		assertThat(organization.status()).isEqualTo(OrganizationStatus.SUSPENDED);
		assertThat(organization.auditMetadata().updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(organization.pullDomainEvents())
				.singleElement()
				.isInstanceOfSatisfying(OrganizationStatusChanged.class, event -> {
					assertThat(event.previousStatus()).isEqualTo(OrganizationStatus.ACTIVE);
					assertThat(event.currentStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
				});
	}

	@Test
	void transitionsSuspendedToActive() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);

		organization.activate(LATER_AT, SECOND_ACTOR_ID);

		assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organization.pullDomainEvents())
				.singleElement()
				.isInstanceOfSatisfying(OrganizationStatusChanged.class, event -> {
					assertThat(event.previousStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
					assertThat(event.currentStatus()).isEqualTo(OrganizationStatus.ACTIVE);
				});
	}

	@Test
	void transitionsActiveAndSuspendedToArchived() {
		Organization active = OrganizationTestFixtures.newOrganization();
		active.pullDomainEvents();
		active.archive(UPDATED_AT, SECOND_ACTOR_ID);
		assertThat(active.status()).isEqualTo(OrganizationStatus.ARCHIVED);

		Organization suspended = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);
		suspended.archive(LATER_AT, SECOND_ACTOR_ID);
		assertThat(suspended.status()).isEqualTo(OrganizationStatus.ARCHIVED);
		assertThat(suspended.pullDomainEvents())
				.singleElement()
				.isInstanceOfSatisfying(OrganizationStatusChanged.class, event -> {
					assertThat(event.previousStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
					assertThat(event.currentStatus()).isEqualTo(OrganizationStatus.ARCHIVED);
				});
	}

	@Test
	void repeatedLifecycleCallsAreNoOps() {
		Organization active = OrganizationTestFixtures.newOrganization();
		active.pullDomainEvents();
		var activeAudit = active.auditMetadata();
		active.activate(UPDATED_AT, SECOND_ACTOR_ID);
		assertThat(active.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(active.auditMetadata()).isEqualTo(activeAudit);
		assertThat(active.pullDomainEvents()).isEmpty();

		Organization suspended = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);
		var suspendedAudit = suspended.auditMetadata();
		suspended.suspend(LATER_AT, SECOND_ACTOR_ID);
		assertThat(suspended.status()).isEqualTo(OrganizationStatus.SUSPENDED);
		assertThat(suspended.auditMetadata()).isEqualTo(suspendedAudit);
		assertThat(suspended.pullDomainEvents()).isEmpty();

		Organization archived = OrganizationTestFixtures.reconstituted(OrganizationStatus.ARCHIVED);
		var archivedAudit = archived.auditMetadata();
		archived.archive(LATER_AT, SECOND_ACTOR_ID);
		assertThat(archived.status()).isEqualTo(OrganizationStatus.ARCHIVED);
		assertThat(archived.auditMetadata()).isEqualTo(archivedAudit);
		assertThat(archived.pullDomainEvents()).isEmpty();
	}

	@Test
	void archivedOrganizationRejectsActivationAndSuspension() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ARCHIVED);

		assertThatThrownBy(() -> organization.activate(LATER_AT, SECOND_ACTOR_ID))
				.isInstanceOf(ArchivedOrganizationModificationException.class);
		assertThatThrownBy(() -> organization.suspend(LATER_AT, SECOND_ACTOR_ID))
				.isInstanceOf(ArchivedOrganizationModificationException.class);
		assertThat(organization.status()).isEqualTo(OrganizationStatus.ARCHIVED);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void invalidTransitionLeavesStateUnchanged() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.SUSPENDED);
		var auditMetadata = organization.auditMetadata();

		assertThatThrownBy(() -> organization.suspend(UPDATED_AT.minusSeconds(1), SECOND_ACTOR_ID))
				.isInstanceOf(InvalidOrganizationValueException.class);

		assertThat(organization.status()).isEqualTo(OrganizationStatus.SUSPENDED);
		assertThat(organization.auditMetadata()).isEqualTo(auditMetadata);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void statusChangedEventRejectsIdenticalStatuses() {
		assertThatThrownBy(() -> new OrganizationStatusChanged(
				OrganizationTestFixtures.ORGANIZATION_ID,
				OrganizationTestFixtures.ORGANIZATION_ID,
				OrganizationTestFixtures.TENANT_ID,
				UPDATED_AT,
				SECOND_ACTOR_ID,
				OrganizationStatus.ACTIVE,
				OrganizationStatus.ACTIVE))
				.isInstanceOf(OrganizationStateConflictException.class);
	}
}
