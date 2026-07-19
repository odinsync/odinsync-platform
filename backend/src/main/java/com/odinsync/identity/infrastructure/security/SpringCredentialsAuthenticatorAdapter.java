package com.odinsync.identity.infrastructure.security;

import com.odinsync.identity.application.port.out.CredentialsAuthenticatorPort;
import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringCredentialsAuthenticatorAdapter implements CredentialsAuthenticatorPort {

	private final AuthenticationManager authenticationManager;

	/**
	 * Delegates credential verification to Spring Security and returns OdinSync's authenticated-user model.
	 */
	@Override
	public AuthenticatedUser authenticate(String email, String password) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(email, password));
			if (authentication.getPrincipal() instanceof OdinSyncUserDetails userDetails) {
				return userDetails.toAuthenticatedUser();
			}
			throw new InvalidCredentialsException();
		} catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}
	}
}
