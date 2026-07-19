package com.odinsync.identity.presentation.controller;

import java.util.List;
import java.util.UUID;

import com.odinsync.identity.application.command.LogoutCommand;
import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.model.ActiveSession;
import com.odinsync.identity.application.model.SessionMetadata;
import com.odinsync.identity.application.port.in.RefreshTokenPort;
import com.odinsync.identity.application.model.RefreshTokenResult;
import com.odinsync.identity.presentation.dto.ActiveSessionResponse;
import com.odinsync.identity.presentation.dto.LogoutRequest;
import com.odinsync.identity.presentation.dto.RefreshTokenRequest;
import com.odinsync.identity.presentation.dto.RefreshTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class RefreshTokenController {

	private final RefreshTokenPort refreshTokenPort;

	/**
	 * Accepts an opaque refresh token and returns a newly rotated access/refresh token pair.
	 */
	@PostMapping("/refresh")
	RefreshTokenResponse refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		RefreshTokenResult result = refreshTokenPort.refresh(new RefreshTokenCommand(
				request.refreshToken(),
				sessionMetadata(httpRequest)));
		return new RefreshTokenResponse(
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
	 * Logs out the current device by revoking the refresh-token family submitted by the client.
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody LogoutRequest request) {
		refreshTokenPort.logout(new LogoutCommand(request.refreshToken()));
	}

	/**
	 * Logs out all active sessions for the authenticated user within the current tenant.
	 */
	@PostMapping("/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(@AuthenticationPrincipal Jwt jwt) {
		refreshTokenPort.logoutAll(userId(jwt), tenantId(jwt));
	}

	/**
	 * Lists safe session metadata for the authenticated user and tenant.
	 */
	@GetMapping("/sessions")
	List<ActiveSessionResponse> sessions(@AuthenticationPrincipal Jwt jwt) {
		return refreshTokenPort.activeSessions(userId(jwt), tenantId(jwt))
				.stream()
				.map(RefreshTokenController::toResponse)
				.toList();
	}

	/**
	 * Revokes one selected session when it belongs to the authenticated user and tenant.
	 */
	@DeleteMapping("/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revokeSession(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal Jwt jwt) {
		refreshTokenPort.revokeSession(sessionId, userId(jwt), tenantId(jwt));
	}

	/**
	 * Maps application session metadata to the public response without credential fields.
	 */
	private static ActiveSessionResponse toResponse(ActiveSession session) {
		return new ActiveSessionResponse(
				session.sessionId(),
				session.familyId(),
				session.deviceName(),
				session.userAgent(),
				session.ipAddress(),
				session.issuedAt(),
				session.lastUsedAt(),
				session.expiresAt(),
				session.currentSession());
	}

	/**
	 * Extracts request metadata used only for session audit and display.
	 */
	private static SessionMetadata sessionMetadata(HttpServletRequest request) {
		return new SessionMetadata(
				request.getHeader("X-Device-Name"),
				request.getHeader("User-Agent"),
				request.getRemoteAddr());
	}

	/**
	 * Reads the authenticated user id from the JWT subject claim.
	 */
	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	/**
	 * Reads the tenant id from OdinSync's tenant JWT claim.
	 */
	private static UUID tenantId(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("tenant_id"));
	}
}
