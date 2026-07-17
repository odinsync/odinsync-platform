package com.odinsync.identity.application.port.out;

import com.odinsync.identity.application.usecase.AuthenticatedUser;
import com.odinsync.identity.application.usecase.GeneratedAccessToken;

public interface AccessTokenGeneratorPort {

	GeneratedAccessToken generate(AuthenticatedUser user);
}
