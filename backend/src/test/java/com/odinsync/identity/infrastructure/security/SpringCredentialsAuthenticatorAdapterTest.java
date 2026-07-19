package com.odinsync.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.domain.exception.InvalidCredentialsException;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SpringCredentialsAuthenticatorAdapterTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@InjectMocks
	private SpringCredentialsAuthenticatorAdapter authenticator;

	@Test
	void sendsUnauthenticatedUsernamePasswordTokenToAuthenticationManager() {
		OdinSyncUserDetails userDetails = userDetails();
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						userDetails,
						null,
						userDetails.getAuthorities()));

		authenticator.authenticate("owner@example.com", "Password@123");

		ArgumentCaptor<Authentication> authenticationCaptor =
				ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(authenticationCaptor.capture());
		Authentication authentication = authenticationCaptor.getValue();
		assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
		assertThat(authentication.isAuthenticated()).isFalse();
		assertThat(authentication.getName()).isEqualTo("owner@example.com");
		assertThat(authentication.getCredentials()).isEqualTo("Password@123");
	}

	@Test
	void convertsOdinSyncUserDetailsToAuthenticatedUser() {
		OdinSyncUserDetails userDetails = userDetails();
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						userDetails,
						null,
						userDetails.getAuthorities()));

		AuthenticatedUser authenticatedUser = authenticator.authenticate(
				"owner@example.com",
				"Password@123");

		assertThat(authenticatedUser.userId()).isEqualTo(userDetails.toAuthenticatedUser().userId());
		assertThat(authenticatedUser.tenantId()).isEqualTo(userDetails.toAuthenticatedUser().tenantId());
		assertThat(authenticatedUser.email()).isEqualTo("owner@example.com");
		assertThat(authenticatedUser.roles()).containsExactly("OWNER");
		assertThat(authenticatedUser.userStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(authenticatedUser.tenantStatus()).isEqualTo(TenantStatus.ACTIVE);
	}

	@Test
	void convertsBadCredentialsExceptionToInvalidCredentialsException() {
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(new BadCredentialsException("bad credentials"));

		assertThatThrownBy(() -> authenticator.authenticate(
				"owner@example.com",
				"WrongPassword"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void convertsOtherAuthenticationExceptionsToInvalidCredentialsException() {
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(new DisabledException("disabled"));

		assertThatThrownBy(() -> authenticator.authenticate(
				"owner@example.com",
				"Password@123"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void rejectsUnexpectedAuthenticatedPrincipalType() {
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						"owner@example.com",
						null,
						List.of()));

		assertThatThrownBy(() -> authenticator.authenticate(
				"owner@example.com",
				"Password@123"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	private static OdinSyncUserDetails userDetails() {
		return new OdinSyncUserDetails(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"owner@example.com",
				"$2a$10$passwordHash",
				List.of("OWNER"),
				UserStatus.ACTIVE,
				TenantStatus.ACTIVE);
	}
}
