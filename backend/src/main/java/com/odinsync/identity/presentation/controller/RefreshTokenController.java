package com.odinsync.identity.presentation.controller;

import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.port.in.RefreshTokenPort;
import com.odinsync.identity.application.usecase.RefreshTokenResult;
import com.odinsync.identity.presentation.dto.RefreshTokenRequest;
import com.odinsync.identity.presentation.dto.RefreshTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class RefreshTokenController {

	private final RefreshTokenPort refreshTokenPort;

	@PostMapping("/refresh")
	RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		RefreshTokenResult result = refreshTokenPort.refresh(new RefreshTokenCommand(request.refreshToken()));
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
}
