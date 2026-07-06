package com.odinsync.identity.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
		@NotBlank
		@Size(max = 150)
		String organizationName,

		@Size(max = 200)
		String legalName,

		@NotBlank
		@Size(max = 150)
		String ownerName,

		@NotBlank
		@Email
		@Size(max = 150)
		String email,

		@NotBlank
		@Size(min = 8, max = 72)
		String password) {
}
