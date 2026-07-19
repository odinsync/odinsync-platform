package com.odinsync.identity.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.odinsync.identity.application.port.out.PasswordEncoderPort;

@Component
@RequiredArgsConstructor
class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

	private final PasswordEncoder passwordEncoder;

	/**
	 * Hashes a raw password using the configured Spring Security password encoder.
	 */
	@Override
	public String encode(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
}
