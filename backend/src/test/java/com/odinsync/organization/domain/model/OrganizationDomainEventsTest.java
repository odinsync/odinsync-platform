package com.odinsync.organization.domain.model;

import static com.odinsync.organization.domain.model.OrganizationTestFixtures.LATER_AT;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.SECOND_ACTOR_ID;
import static com.odinsync.organization.domain.model.OrganizationTestFixtures.UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.event.OrganizationCreated;
import com.odinsync.organization.domain.event.OrganizationProfileUpdated;
import com.odinsync.organization.domain.event.OrganizationSettingsUpdated;
import com.odinsync.organization.domain.event.OrganizationStatusChanged;
import com.odinsync.organization.domain.exception.ArchivedOrganizationModificationException;
import org.junit.jupiter.api.Test;

class OrganizationDomainEventsTest {

	@Test
	void pullReturnsImmutableEventsAndClearsInternalCollection() {
		Organization organization = OrganizationTestFixtures.newOrganization();

		var events = organization.pullDomainEvents();

		assertThat(events).singleElement().isInstanceOf(OrganizationCreated.class);
		assertThatThrownBy(() -> events.add((OrganizationCreated) events.getFirst()))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThat(organization.pullDomainEvents()).isEmpty();
	}

	@Test
	void preservesEventOrderAcrossSuccessfulOperations() {
		Organization organization = OrganizationTestFixtures.newOrganization();
		organization.updateProfile(
				OrganizationTestFixtures.changedName(),
				organization.taxRegistrationNumber(),
				organization.address(),
				organization.contact(),
				UPDATED_AT,
				SECOND_ACTOR_ID);
		organization.updateSettings(OrganizationTestFixtures.changedSettings(), LATER_AT, SECOND_ACTOR_ID);
		organization.archive(LATER_AT, SECOND_ACTOR_ID);

		assertThat(organization.pullDomainEvents())
				.extracting(Object::getClass)
				.containsExactly(
						OrganizationCreated.class,
						OrganizationProfileUpdated.class,
						OrganizationSettingsUpdated.class,
						OrganizationStatusChanged.class);
	}

	@Test
	void noOpAndFailedOperationsAddNoEvents() {
		Organization organization = OrganizationTestFixtures.reconstituted(OrganizationStatus.ARCHIVED);

		organization.archive(LATER_AT, SECOND_ACTOR_ID);
		assertThatThrownBy(() -> organization.updateSettings(OrganizationTestFixtures.changedSettings(), LATER_AT, SECOND_ACTOR_ID))
				.isInstanceOf(ArchivedOrganizationModificationException.class);

		assertThat(organization.pullDomainEvents()).isEmpty();
	}
}
