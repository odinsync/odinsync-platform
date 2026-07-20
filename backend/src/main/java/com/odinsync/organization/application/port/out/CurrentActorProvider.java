package com.odinsync.organization.application.port.out;

import com.odinsync.organization.application.model.AuthenticatedActor;

public interface CurrentActorProvider {

	AuthenticatedActor getCurrentActor();
}
