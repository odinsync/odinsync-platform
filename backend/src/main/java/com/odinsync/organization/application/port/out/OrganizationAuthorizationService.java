package com.odinsync.organization.application.port.out;

import com.odinsync.organization.application.model.AuthenticatedActor;

public interface OrganizationAuthorizationService {

	void requireProfileRead(AuthenticatedActor actor);

	void requireProfileUpdate(AuthenticatedActor actor);

	void requireSettingsRead(AuthenticatedActor actor);

	void requireSettingsUpdate(AuthenticatedActor actor);
}
