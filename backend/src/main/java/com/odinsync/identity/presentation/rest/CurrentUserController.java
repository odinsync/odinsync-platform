package com.odinsync.identity.presentation.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {

	@GetMapping("/me")
	CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return new CurrentUserResponse(
				UUID.fromString(jwt.getSubject()),
				UUID.fromString(jwt.getClaimAsString("tenant_id")),
				jwt.getClaimAsString("email"),
				safeRoles(jwt));
	}

	private static List<String> safeRoles(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList("roles");
		return roles == null ? List.of() : List.copyOf(roles);
	}
}
