package com.odinsync.identity.presentation.controller;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.model.SessionMetadata;
import com.odinsync.identity.application.port.in.LoginPort;
import com.odinsync.identity.application.model.LoginResult;
import com.odinsync.identity.presentation.dto.LoginRequest;
import com.odinsync.identity.presentation.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class LoginController {

	private final LoginPort loginPort;

	/**
	 * Authenticates credentials and returns an access token plus initial refresh token.
	 */
	@PostMapping("/login")
	LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		LoginResult result = loginPort.login(new LoginCommand(
				request.email(),
				request.password(),
				sessionMetadata(httpRequest)));

		return new LoginResponse(
				result.accessToken(),
				result.tokenType(),
				result.expiresIn(),
				result.refreshToken(),
				result.refreshTokenExpiresAt(),
				result.tenantId(),
				result.userId(),
				result.roles());
	}

	/**
	 * Extracts request metadata used to label and audit the new refresh-token session.
	 */
	private static SessionMetadata sessionMetadata(HttpServletRequest request) {
		return new SessionMetadata(
				request.getHeader("X-Device-Name"),
				request.getHeader("User-Agent"),
				request.getRemoteAddr());
	}
}
