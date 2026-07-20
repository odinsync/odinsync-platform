package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.query.GetOrganizationProfileQuery;
import com.odinsync.organization.application.result.OrganizationProfileResult;

public interface GetOrganizationProfileUseCase {

	OrganizationProfileResult get(GetOrganizationProfileQuery query);
}
