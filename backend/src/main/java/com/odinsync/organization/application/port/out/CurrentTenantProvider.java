package com.odinsync.organization.application.port.out;

import java.util.UUID;

public interface CurrentTenantProvider {

	UUID getCurrentTenantId();
}
