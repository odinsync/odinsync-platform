package com.odinsync.identity.presentation.rest;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary endpoint set for verifying RBAC infrastructure until real business endpoints own these rules.
 */
@RestController
@RequestMapping("/api/v1/security-test")
public class SecurityAuthorizationTestController {

	@GetMapping("/authenticated")
	Map<String, String> authenticated() {
		return Map.of("message", "Authenticated endpoint accessed");
	}

	@GetMapping("/owner")
	@PreAuthorize("hasRole('OWNER')")
	Map<String, String> owner() {
		return Map.of("message", "OWNER endpoint accessed");
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	Map<String, String> admin() {
		return Map.of("message", "ADMIN endpoint accessed");
	}

	@GetMapping("/owner-or-admin")
	@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
	Map<String, String> ownerOrAdmin() {
		return Map.of("message", "OWNER or ADMIN endpoint accessed");
	}

	@GetMapping("/employee")
	@PreAuthorize("hasRole('EMPLOYEE')")
	Map<String, String> member() {
		return Map.of("message", "EMPLOYEE endpoint accessed");
	}
}
