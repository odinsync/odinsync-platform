package com.odinsync.organization.application.port.in;

import com.odinsync.organization.application.query.GetOrganizationSettingsQuery;
import com.odinsync.organization.application.result.OrganizationSettingsResult;

public interface GetOrganizationSettingsUseCase {

	OrganizationSettingsResult get(GetOrganizationSettingsQuery query);
}
