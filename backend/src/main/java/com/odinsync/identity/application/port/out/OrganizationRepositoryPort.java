package com.odinsync.identity.application.port.out;

import com.odinsync.identity.domain.model.Organization;

public interface OrganizationRepositoryPort {

	Organization save(Organization organization);
}
