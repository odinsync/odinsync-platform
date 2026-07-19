package com.odinsync.shared.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties({
		OdinSyncJwtProperties.class,
		RefreshTokenProperties.class
})
class JwtKeyConfiguration {

	/**
	 * Builds the RSA key pair used to sign and validate OdinSync JWT access tokens.
	 */
	@Bean
	KeyPair jwtKeyPair(OdinSyncJwtProperties properties, ResourceLoader resourceLoader) {
		if (properties.generateDevelopmentKeys()) {
			return generateDevelopmentKeyPair();
		}
		if (isBlank(properties.privateKeyLocation()) || isBlank(properties.publicKeyLocation())) {
			throw new IllegalStateException(
					"JWT RSA key locations must be configured or development key generation must be enabled.");
		}
		return new KeyPair(
				readPublicKey(properties.publicKeyLocation(), resourceLoader),
				readPrivateKey(properties.privateKeyLocation(), resourceLoader));
	}

	/**
	 * Creates the JWT encoder that signs outgoing access tokens with RS256.
	 */
	@Bean
	JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
		RSAPublicKey publicKey = (RSAPublicKey) jwtKeyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) jwtKeyPair.getPrivate();
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
	}

	/**
	 * Creates the JWT decoder that validates incoming access tokens and issuer claims.
	 */
	@Bean
	JwtDecoder jwtDecoder(KeyPair jwtKeyPair, OdinSyncJwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

	/**
	 * Returns whether a configuration value is absent or blank.
	 */
	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Generates an in-memory RSA key pair for local development and tests.
	 */
	private static KeyPair generateDevelopmentKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to generate development JWT key pair", exception);
		}
	}

	/**
	 * Reads and parses a PKCS#8 RSA private key from a configured resource location.
	 */
	private static RSAPrivateKey readPrivateKey(String location, ResourceLoader resourceLoader) {
		try {
			String pem = readPem(location, resourceLoader)
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "");
			byte[] decoded = Base64.getMimeDecoder().decode(pem);
			return (RSAPrivateKey) KeyFactory.getInstance("RSA")
					.generatePrivate(new PKCS8EncodedKeySpec(decoded));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to read JWT private key", exception);
		}
	}

	/**
	 * Reads and parses an X.509 RSA public key from a configured resource location.
	 */
	private static RSAPublicKey readPublicKey(String location, ResourceLoader resourceLoader) {
		try {
			String pem = readPem(location, resourceLoader)
					.replace("-----BEGIN PUBLIC KEY-----", "")
					.replace("-----END PUBLIC KEY-----", "");
			byte[] decoded = Base64.getMimeDecoder().decode(pem);
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(decoded));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to read JWT public key", exception);
		}
	}

	/**
	 * Loads PEM text from Spring's resource abstraction.
	 */
	private static String readPem(String location, ResourceLoader resourceLoader) {
		Resource resource = resourceLoader.getResource(location);
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException("Unable to read JWT key: " + location, exception);
		}
	}
}
