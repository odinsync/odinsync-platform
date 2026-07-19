package com.odinsync.identity.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

import com.odinsync.identity.application.port.out.AccessTokenGeneratorPort;
import com.odinsync.identity.application.model.AuthenticatedUser;
import com.odinsync.identity.application.model.GeneratedAccessToken;
import com.odinsync.shared.security.OdinSyncJwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JwtAccessTokenGeneratorAdapter implements AccessTokenGeneratorPort {

	private final JwtEncoder jwtEncoder;
	private final OdinSyncJwtProperties jwtProperties;

	@Override
	public GeneratedAccessToken generate(AuthenticatedUser user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.userId().toString())
				.id(UUID.randomUUID().toString())
				.claim("tenant_id", user.tenantId().toString())
				.claim("email", user.email())
				.claim("roles", user.roles())
				.build();

		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims))
				.getTokenValue();
		return new GeneratedAccessToken(token, jwtProperties.accessTokenTtl().toSeconds());
	}
}
