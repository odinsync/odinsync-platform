package com.odinsync.identity.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		@NotBlank
		String refreshToken) {
}
