package com.odinsync.organization.application.service;

import static com.odinsync.organization.application.ApplicationContractTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.TENANT_ID;
import static com.odinsync.organization.application.ApplicationContractTestFixtures.organization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.odinsync.organization.application.exception.OrganizationNotFoundException;
import com.odinsync.organization.application.exception.MissingTenantContextException;
import com.odinsync.organization.application.mapper.OrganizationProfileMapper;
import com.odinsync.organization.application.port.out.CurrentTenantProvider;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.query.GetOrganizationProfileQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrganizationProfileServiceTest {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private CurrentTenantProvider currentTenantProvider;

	private final OrganizationProfileMapper profileMapper = new OrganizationProfileMapper();

	private GetOrganizationProfileService service;

	@BeforeEach
	void setUp() {
		service = new GetOrganizationProfileService(
				organizationRepository,
				currentTenantProvider,
				profileMapper);
	}

	@Test
	void getsProfileForAuthenticatedTenant() {
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenReturn(Optional.of(organization()));

		var result = service.get(new GetOrganizationProfileQuery(ORGANIZATION_ID));

		assertThat(result.tenantId()).isEqualTo(TENANT_ID);
		assertThat(result.name()).isEqualTo(organization().name());
		verify(organizationRepository).findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID);
	}

	@Test
	void throwsWhenOrganizationIsMissing() {
		when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
		when(organizationRepository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(new GetOrganizationProfileQuery(ORGANIZATION_ID)))
				.isInstanceOf(OrganizationNotFoundException.class);
	}

	@Test
	void tenantProviderFailurePreventsRepositoryAccess() {
		MissingTenantContextException missingTenant = new MissingTenantContextException();
		when(currentTenantProvider.getCurrentTenantId()).thenThrow(missingTenant);

		assertThatThrownBy(() -> service.get(new GetOrganizationProfileQuery(ORGANIZATION_ID)))
				.isSameAs(missingTenant);

		verifyNoInteractions(organizationRepository);
	}

	@Test
	void rejectsNullQuery() {
		assertThatThrownBy(() -> service.get(null))
				.isInstanceOf(NullPointerException.class);
	}
}
