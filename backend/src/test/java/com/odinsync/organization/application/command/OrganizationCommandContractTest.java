package com.odinsync.organization.application.command;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.address;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.contact;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.name;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.settings;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.taxRegistrationNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrganizationCommandContractTest {

	@Test
	void createsProfileAndSettingsCommands() {
		CreateOrganizationCommand createCommand =
				new CreateOrganizationCommand(name(), taxRegistrationNumber(), address(), contact(), settings());
		UpdateOrganizationProfileCommand profileCommand =
				new UpdateOrganizationProfileCommand(name(), taxRegistrationNumber(), address(), contact());
		UpdateOrganizationSettingsCommand settingsCommand = new UpdateOrganizationSettingsCommand(settings());

		assertThat(createCommand.name()).isEqualTo(name());
		assertThat(profileCommand.contact()).isEqualTo(contact());
		assertThat(settingsCommand.settings()).isEqualTo(settings());
	}

	@Test
	void createsLifecycleCommands() {
		assertThat(new ActivateOrganizationCommand(ORGANIZATION_ID).organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(new SuspendOrganizationCommand(ORGANIZATION_ID).organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(new ArchiveOrganizationCommand(ORGANIZATION_ID).organizationId()).isEqualTo(ORGANIZATION_ID);
	}

	@Test
	void rejectsNullCommandArguments() {
		assertThatThrownBy(() -> new CreateOrganizationCommand(
				null,
				taxRegistrationNumber(),
				address(),
				contact(),
				settings()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new UpdateOrganizationProfileCommand(name(), null, address(), contact()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new UpdateOrganizationSettingsCommand(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new ActivateOrganizationCommand(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new SuspendOrganizationCommand(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new ArchiveOrganizationCommand(null))
				.isInstanceOf(NullPointerException.class);
	}
}
