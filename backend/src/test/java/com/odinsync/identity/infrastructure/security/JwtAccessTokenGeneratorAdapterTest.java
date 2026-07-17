package com.odinsync.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.odinsync.identity.application.usecase.AuthenticatedUser;
import com.odinsync.identity.application.usecase.GeneratedAccessToken;
import com.odinsync.identity.domain.model.TenantStatus;
import com.odinsync.identity.domain.model.UserStatus;
import com.odinsync.shared.security.OdinSyncJwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtAccessTokenGeneratorAdapterTest {

	@Mock
	private JwtEncoder jwtEncoder;

	private JwtAccessTokenGeneratorAdapter tokenGenerator;

	@Test
	void generatesJwtWithExpectedClaimsAndExpiry() {
		UUID userId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
				userId,
				tenantId,
				"owner@example.com",
				List.of("OWNER", "ADMIN"),
				UserStatus.ACTIVE,
				TenantStatus.ACTIVE);
		OdinSyncJwtProperties jwtProperties = new OdinSyncJwtProperties(
				"odinsync-platform",
				Duration.ofMinutes(15),
				null,
				null,
				true);
		tokenGenerator = new JwtAccessTokenGeneratorAdapter(jwtEncoder, jwtProperties);
		when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());

		GeneratedAccessToken token = tokenGenerator.generate(authenticatedUser);

		assertThat(token.value()).isEqualTo("signed-jwt");
		assertThat(token.expiresIn()).isEqualTo(900);
		ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
				ArgumentCaptor.forClass(JwtEncoderParameters.class);
		verify(jwtEncoder).encode(parametersCaptor.capture());
		JwtEncoderParameters parameters = parametersCaptor.getValue();
		assertThat(parameters.getJwsHeader().getAlgorithm().getName()).isEqualTo("RS256");
		assertThat((String) parameters.getClaims().getClaim("iss")).isEqualTo("odinsync-platform");
		assertThat(parameters.getClaims().getSubject()).isEqualTo(userId.toString());
		assertThat((String) parameters.getClaims().getClaim("tenant_id")).isEqualTo(tenantId.toString());
		assertThat((String) parameters.getClaims().getClaim("email")).isEqualTo("owner@example.com");
		List<?> roles = parameters.getClaims().getClaim("roles");
		assertThat(roles).isEqualTo(List.of("OWNER", "ADMIN"));
		assertThat(parameters.getClaims().getIssuedAt()).isNotNull();
		assertThat(parameters.getClaims().getExpiresAt()).isNotNull();
		assertThat(parameters.getClaims().getId()).isNotBlank();
	}

	private static Jwt jwt() {
		Instant issuedAt = Instant.now();
		return new Jwt(
				"signed-jwt",
				issuedAt,
				issuedAt.plusSeconds(900),
				Map.of("alg", "RS256"),
				Map.of("sub", UUID.randomUUID().toString()));
	}
}
