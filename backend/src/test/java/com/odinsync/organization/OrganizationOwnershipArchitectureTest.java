package com.odinsync.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrganizationOwnershipArchitectureTest {

	@Test
	void identityDoesNotDependOnOrganizationInfrastructure() throws IOException {
		List<String> violations = javaSourceFiles(Path.of("src/main/java/com/odinsync/identity"))
				.stream()
				.flatMap(path -> List.of(
								"com.odinsync.organization.infrastructure",
								"SpringDataOrganizationRepository",
								"OrganizationRepositoryAdapter",
								"com.odinsync.organization.infrastructure.persistence.entity",
								"com.odinsync.organization.infrastructure.persistence.mapper")
						.stream()
						.filter(pattern -> contains(path, pattern))
						.map(pattern -> path + " contains " + pattern))
				.toList();

		assertThat(violations).isEmpty();
	}

	@Test
	void organizationDomainDoesNotDependOnIdentity() throws IOException {
		List<String> violations = javaSourceFiles(Path.of("src/main/java/com/odinsync/organization/domain"))
				.stream()
				.filter(path -> contains(path, "com.odinsync.identity"))
				.map(Path::toString)
				.toList();

		assertThat(violations).isEmpty();
	}

	@Test
	void identityDoesNotOwnOrganizationPersistenceClasses() throws IOException {
		List<String> violations = javaSourceFiles(Path.of("src/main/java/com/odinsync/identity"))
				.stream()
				.filter(path -> path.getFileName().toString().contains("Organization"))
				.filter(path -> path.toString().contains("/persistence/") || contains(path, "OrganizationRepositoryPort"))
				.map(Path::toString)
				.toList();

		assertThat(violations).isEmpty();
	}

	private static List<Path> javaSourceFiles(Path root) throws IOException {
		try (var paths = Files.walk(root)) {
			return paths
					.filter(path -> path.toString().endsWith(".java"))
					.toList();
		}
	}

	private static boolean contains(Path path, String pattern) {
		try {
			return Files.readString(path).contains(pattern);
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to read " + path, exception);
		}
	}
}
