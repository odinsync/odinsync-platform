package com.odinsync.identity.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.odinsync.identity.application.command.RegisterOrganizationCommand;
import com.odinsync.identity.application.port.in.RegisterOrganizationPort;
import com.odinsync.identity.application.usecase.RegisterOrganizationResult;
import com.odinsync.identity.presentation.dto.RegisterOrganizationRequest;
import com.odinsync.identity.presentation.dto.RegisterOrganizationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class RegisterOrganizationController {
	private final RegisterOrganizationPort registerOrganizationUseCase;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	RegisterOrganizationResponse register(
			@Valid @RequestBody RegisterOrganizationRequest request
	) {
		RegisterOrganizationCommand command = new RegisterOrganizationCommand(
				request.organizationName(),
				request.legalName(),
				request.ownerName(),
				request.email(),
				request.password()
		);

		RegisterOrganizationResult result = registerOrganizationUseCase.register(command);
		return new RegisterOrganizationResponse(
				result.tenantId(),
				result.organizationId(),
				result.userId(),
				result.message());
	}
}
