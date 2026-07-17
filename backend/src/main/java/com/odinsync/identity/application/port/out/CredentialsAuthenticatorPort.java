package com.odinsync.identity.application.port.out;

import com.odinsync.identity.application.usecase.AuthenticatedUser;

public interface CredentialsAuthenticatorPort {

	AuthenticatedUser authenticate(String email, String password);
}
