package com.odinsync;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import com.odinsync.identity.application.port.out.OrganizationRepositoryPort;
import com.odinsync.identity.application.port.out.PasswordEncoderPort;
import com.odinsync.identity.application.port.out.RoleRepositoryPort;
import com.odinsync.identity.application.port.out.TenantRepositoryPort;
import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.application.port.out.UserRoleRepositoryPort;
import com.odinsync.identity.domain.model.Role;

@SpringBootTest(
		classes = OdinsyncPlatformApplicationTests.TestApplication.class,
		properties = {
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
	@ComponentScan(
			basePackages = "com.odinsync",
			excludeFilters = @ComponentScan.Filter(
					type = FilterType.REGEX,
					pattern = "com\\.odinsync\\.identity\\.infrastructure\\.persistence\\.repository\\..*"))
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
			return new RoleRepositoryPort() {
				@Override
				public Optional<com.odinsync.identity.domain.model.Role> findByTenantIdAndName(
						java.util.UUID tenantId,
						String name) {
					return Optional.empty();
				}

				@Override
				public com.odinsync.identity.domain.model.Role save(
						com.odinsync.identity.domain.model.Role role) {
					return role;
				}
			};
		}

		@Bean
		@Primary
		UserRoleRepositoryPort userRoleRepositoryPort() {
			return new UserRoleRepositoryPort() {
				@Override
				public Role save(Role role) {
					return role;
				}

				@Override
				public void assignRole(java.util.UUID userId, java.util.UUID roleId) {
				}
			};
		}

		@Bean
		@Primary
		PasswordEncoderPort passwordHasher() {
			return rawPassword -> rawPassword;
		}
	}
}
