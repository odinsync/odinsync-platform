package com.odinsync.organization.application.port.out;

import java.util.Collection;

import com.odinsync.organization.domain.event.OrganizationDomainEvent;

public interface OrganizationDomainEventPublisher {

	void publishAll(Collection<? extends OrganizationDomainEvent> events);
}
