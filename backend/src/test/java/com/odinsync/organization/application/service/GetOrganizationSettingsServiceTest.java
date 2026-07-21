package com.odinsync.organization.application.service;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.actor;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.organization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.odinsync.organization.application.exception.MissingActorContextException;
import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.mapper.OrganizationSettingsMapper;
import com.odinsync.organization.application.port.out.CurrentActorProvider;
import com.odinsync.organization.application.port.out.OrganizationAuthorizationService;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.query.GetOrganizationSettingsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrganizationSettingsServiceTest {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@Mock
	private OrganizationAuthorizationService authorizationService;

	private final OrganizationSettingsMapper settingsMapper = new OrganizationSettingsMapper();

	private GetOrganizationSettingsService service;

	@BeforeEach
	void setUp() {
		service = new GetOrganizationSettingsService(
				organizationRepository,
				currentActorProvider,
				authorizationService,
				settingsMapper);
	}

	@Test
	void getsSettingsForActorTenant() {
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(organizationRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(organization()));

		var result = service.get(new GetOrganizationSettingsQuery());

		assertThat(result.tenantId()).isEqualTo(TENANT_ID);
		assertThat(result.settings()).isEqualTo(organization().settings());
		verify(authorizationService).requireSettingsRead(actor());
		verify(organizationRepository).findByTenantId(TENANT_ID);
	}

	@Test
	void unauthorizedAccessPreventsRepositoryAccess() {
		RuntimeException accessDenied = new RuntimeException("denied");
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		doThrow(accessDenied).when(authorizationService).requireSettingsRead(actor());

		assertThatThrownBy(() -> service.get(new GetOrganizationSettingsQuery()))
				.isSameAs(accessDenied);

		verifyNoInteractions(organizationRepository);
	}

	@Test
	void actorProviderFailurePreventsRepositoryAccess() {
		MissingActorContextException missingActor = new MissingActorContextException();
		when(currentActorProvider.getCurrentActor()).thenThrow(missingActor);

		assertThatThrownBy(() -> service.get(new GetOrganizationSettingsQuery()))
				.isSameAs(missingActor);

		verifyNoInteractions(authorizationService, organizationRepository);
	}

	@Test
	void throwsWhenOrganizationIsMissing() {
		when(currentActorProvider.getCurrentActor()).thenReturn(actor());
		when(organizationRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(new GetOrganizationSettingsQuery()))
				.isInstanceOf(OrganizationNotFoundException.class);

		verify(authorizationService).requireSettingsRead(actor());
	}

	@Test
	void rejectsNullQuery() {
		assertThatThrownBy(() -> service.get(null))
				.isInstanceOf(NullPointerException.class);
	}
}
