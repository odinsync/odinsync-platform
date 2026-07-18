package com.odinsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.odinsync.identity.infrastructure.security.OdinSyncJwtAuthenticationConverter;
import com.odinsync.identity.application.port.out.OrganizationRepositoryPort;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleAssignmentPort;

@SpringBootTest(
		classes = OdinsyncPlatformApplicationTests.TestApplication.class,
		properties = {
		"odinsync.security.jwt.generate-development-keys=true",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class OdinsyncPlatformApplicationTests {

	@Test
	void contextLoads() {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@ComponentScan(basePackages = {
			"com.odinsync.shared.security",
			"com.odinsync.shared.exception"
	})
	static class TestApplication {

		@Bean
		@Primary
		TenantRepositoryPort tenantRepositoryPort() {
			return tenant -> tenant;
		}

		@Bean
		@Primary
		OrganizationRepositoryPort organizationRepositoryPort() {
			return organization -> organization;
		}

		@Bean
		@Primary
		UserRepositoryPort userRepositoryPort() {
			return new UserRepositoryPort() {
				@Override
				public boolean existsByEmail(String email) {
					return false;
				}

				@Override
				public com.odinsync.identity.domain.model.User save(
						com.odinsync.identity.domain.model.User user) {
					return user;
				}
			};
		}

		@Bean
		@Primary
		RoleRepositoryPort roleRepositoryPort() {
			return role -> role;
		}

		@Bean
		@Primary
		UserRoleAssignmentPort userRoleAssignmentPort() {
			return (userId, roleId) -> {
			};
		}

		@Bean
		@Primary
		PasswordEncoderPort passwordHasher() {
			return rawPassword -> rawPassword;
		}

		@Bean
		UserDetailsService userDetailsService() {
			return username -> User.withUsername(username)
					.password("{noop}password")
					.roles("TEST")
					.build();
		}

		@Bean
		OdinSyncJwtAuthenticationConverter jwtAuthenticationConverter() {
			return new OdinSyncJwtAuthenticationConverter();
		}
	}
}
