package com.odinsync.organization.infrastructure.config;

import java.time.Clock;
import java.util.UUID;

import com.odinsync.organization.application.port.in.ProvisionOrganizationUseCase;
import com.odinsync.organization.application.port.out.IdGenerator;
import com.odinsync.organization.application.port.out.OrganizationDomainEventPublisher;
import com.odinsync.organization.application.port.out.OrganizationRepository;
import com.odinsync.organization.application.port.out.TimeProvider;
import com.odinsync.organization.application.service.OrganizationProvisioningDefaults;
import com.odinsync.organization.application.service.ProvisionOrganizationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrganizationDefaultsProperties.class)
public class OrganizationApplicationConfiguration {

	@Bean
	ProvisionOrganizationUseCase provisionOrganizationUseCase(
			OrganizationRepository organizationRepository,
			IdGenerator organizationIdGenerator,
			TimeProvider organizationTimeProvider,
			OrganizationDomainEventPublisher organizationDomainEventPublisher,
			OrganizationProvisioningDefaults organizationProvisioningDefaults) {
		return new ProvisionOrganizationService(
				organizationRepository,
				organizationIdGenerator,
				organizationTimeProvider,
				organizationDomainEventPublisher,
				organizationProvisioningDefaults);
	}

	@Bean
	IdGenerator organizationIdGenerator() {
		return UUID::randomUUID;
	}

	@Bean
	TimeProvider organizationTimeProvider(Clock clock) {
		return clock::instant;
	}

	@Bean
	OrganizationProvisioningDefaults organizationProvisioningDefaults(OrganizationDefaultsProperties properties) {
		OrganizationDefaultsProperties.Address address = properties.getAddress();
		return OrganizationProvisioningDefaults.of(
				address.getLine1(),
				address.getLine2(),
				address.getCity(),
				address.getStateOrRegion(),
				address.getPostalCode(),
				address.getCountryCode(),
				properties.getPhone(),
				properties.getCurrency(),
				properties.getLocale(),
				properties.getTimeZone(),
				properties.getDateFormat(),
				properties.getTimeFormat(),
				properties.getWeekStart());
	}

	@Bean
	@ConditionalOnMissingBean(OrganizationDomainEventPublisher.class)
	OrganizationDomainEventPublisher organizationDomainEventPublisher() {
		return events -> {
		};
	}
}
