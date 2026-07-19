package com.odinsync.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.identity.application.usecase.AuthenticatedUser;

public interface AuthenticatedUserLookupPort {

	Optional<AuthenticatedUser> findById(UUID userId);
}
