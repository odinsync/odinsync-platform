package com.odinsync.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrganizationApplicationBoundaryTest {

	@Test
	void applicationContractsDoNotDependOnFrameworkOrInfrastructureTypes() throws IOException {
		Path applicationRoot = Path.of("src/main/java/com/odinsync/organization/application");
		List<String> forbiddenPatterns = List.of(
				"org.springframework",
				"jakarta.persistence",
				"com.fasterxml",
				"SecurityContextHolder",
				"Authentication",
				"Jwt",
				"JpaRepository",
				"EntityManager",
				"ApplicationEventPublisher",
				"@Service",
				"@Component",
				"@Repository",
				"@Transactional",
				"@PreAuthorize");

		try (var paths = Files.walk(applicationRoot)) {
			List<String> violations = paths
					.filter(path -> path.toString().endsWith(".java"))
					.flatMap(path -> forbiddenPatterns.stream()
							.filter(pattern -> contains(path, pattern))
							.map(pattern -> path + " contains " + pattern))
					.toList();

			assertThat(violations).isEmpty();
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
