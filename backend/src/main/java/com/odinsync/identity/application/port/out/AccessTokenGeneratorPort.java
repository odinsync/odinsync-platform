package com.odinsync.identity.application.port.out;

import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;

public interface AccessTokenGeneratorPort {

	GeneratedAccessToken generate(AuthenticatedUser user);
}
