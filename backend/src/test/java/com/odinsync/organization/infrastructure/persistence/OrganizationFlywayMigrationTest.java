package com.odinsync.organization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrganizationFlywayMigrationTest {

	private static final Path MIGRATION = Path.of(
			"src/main/resources/db/migration/V4__align_organization_aggregate_schema.sql");
	private static final Path HARDENING_MIGRATION = Path.of(
			"src/main/resources/db/migration/V5__harden_organization_aggregate_constraints.sql");
	private static final Path IDENTITY_MIGRATION = Path.of(
			"src/main/resources/db/migration/V1__create_identity_access_schema.sql");

	@Test
	void migrationUsesFlywayVersionFourNamingConvention() {
		assertThat(MIGRATION.getFileName().toString())
				.matches("V4__[a-z0-9_]+\\.sql");
	}

	@Test
	void hardeningMigrationUsesFlywayVersionFiveNamingConvention() {
		assertThat(HARDENING_MIGRATION.getFileName().toString())
				.matches("V5__[a-z0-9_]+\\.sql");
	}

	@Test
	void migrationAddsColumnsRequiredByOrganizationAggregateJpaEntity() throws IOException {
		String sql = normalizedMigrationSql();

		assertThat(sql).contains("ALTER COLUMN created_at TYPE TIMESTAMPTZ");
		assertThat(sql).contains("ALTER COLUMN updated_at TYPE TIMESTAMPTZ");
		assertThat(sql).contains("ALTER TABLE organizations");
		assertThat(sql).contains("ADD COLUMN display_name VARCHAR(120)");
		assertThat(sql).contains("ADD COLUMN tax_registration_number VARCHAR(50)");
		assertThat(sql).contains("ADD COLUMN address_line1 VARCHAR(200)");
		assertThat(sql).contains("ADD COLUMN address_line2 VARCHAR(200)");
		assertThat(sql).contains("ADD COLUMN address_city VARCHAR(100)");
		assertThat(sql).contains("ADD COLUMN address_state_or_region VARCHAR(100)");
		assertThat(sql).contains("ADD COLUMN address_postal_code VARCHAR(20)");
		assertThat(sql).contains("ADD COLUMN address_country_code VARCHAR(2)");
		assertThat(sql).contains("ADD COLUMN contact_email VARCHAR(254)");
		assertThat(sql).contains("ADD COLUMN contact_phone VARCHAR(30)");
		assertThat(sql).contains("ADD COLUMN contact_website VARCHAR(500)");
		assertThat(sql).contains("ADD COLUMN currency_code VARCHAR(3)");
		assertThat(sql).contains("ADD COLUMN time_zone VARCHAR(100)");
		assertThat(sql).contains("ADD COLUMN locale VARCHAR(20)");
		assertThat(sql).contains("ADD COLUMN date_format VARCHAR(30)");
		assertThat(sql).contains("ADD COLUMN time_format VARCHAR(20)");
		assertThat(sql).contains("ADD COLUMN week_start VARCHAR(15)");
		assertThat(sql).contains("ADD COLUMN status VARCHAR(30)");
		assertThat(sql).contains("ADD COLUMN created_by UUID");
		assertThat(sql).contains("ADD COLUMN updated_by UUID");
		assertThat(sql).contains("ADD COLUMN version BIGINT");
	}

	@Test
	void migrationBackfillsExistingIdentityOwnedRowsBeforeEnforcingNullability() throws IOException {
		String sql = normalizedMigrationSql();

		assertThat(sql).contains("UPDATE organizations");
		assertThat(sql).contains("legal_name = COALESCE(NULLIF(TRIM(legal_name), ''), NULLIF(TRIM(name), ''), 'Not provided')");
		assertThat(sql).contains("display_name = COALESCE(NULLIF(TRIM(name), ''), NULLIF(TRIM(display_name), ''), 'Not provided')");
		assertThat(sql).contains("contact_email = LOWER(COALESCE(NULLIF(TRIM(email), ''), NULLIF(TRIM(contact_email), ''), 'unknown@example.com'))");
		assertThat(sql).contains("tax_registration_number = COALESCE(NULLIF(TRIM(tax_registration_number), ''), NULLIF(TRIM(gst_number), ''))");
		assertThat(positionOf(sql, "UPDATE organizations"))
				.isLessThan(positionOf(sql, "ALTER COLUMN legal_name SET NOT NULL"));
	}

	@Test
	void migrationEnforcesAggregateNullabilityAndCheckConstraints() throws IOException {
		String sql = normalizedMigrationSql();

		List<String> requiredNotNullColumns = List.of(
				"legal_name",
				"display_name",
				"address_line1",
				"address_city",
				"address_state_or_region",
				"address_postal_code",
				"address_country_code",
				"contact_email",
				"contact_phone",
				"currency_code",
				"time_zone",
				"locale",
				"date_format",
				"time_format",
				"week_start",
				"status",
				"created_by",
				"updated_by",
				"version");

		requiredNotNullColumns.forEach(column ->
				assertThat(sql).contains("ALTER COLUMN " + column + " SET NOT NULL"));
		assertThat(sql).contains("chk_organizations_status");
		assertThat(sql).contains("chk_organizations_date_format");
		assertThat(sql).contains("chk_organizations_time_format");
		assertThat(sql).contains("chk_organizations_week_start");
		assertThat(sql).contains("chk_organizations_audit_chronology");
		assertThat(sql).contains("chk_organizations_version");
	}

	@Test
	void migrationKeepsLegacyIdentityColumnsForCompatibility() throws IOException {
		String sql = normalizedMigrationSql();

		assertThat(sql).doesNotContain("DROP COLUMN name");
		assertThat(sql).doesNotContain("DROP COLUMN email");
		assertThat(sql).doesNotContain("DROP COLUMN gst_number");
		assertThat(sql).doesNotContain("DROP COLUMN address");
		assertThat(sql).contains("DEFAULT 'unknown@example.com'");
		assertThat(sql).contains("DEFAULT '00000000-0000-0000-0000-000000000000'");
	}

	@Test
	void migrationReliesOnExistingTenantUniquenessForTenantScopedQueries() throws IOException {
		String sql = normalizedMigrationSql();
		String identitySql = normalizedSql(IDENTITY_MIGRATION);

		assertThat(identitySql).contains("CREATE UNIQUE INDEX uk_organizations_tenant_id ON organizations (tenant_id)");
		assertThat(sql).doesNotContain("idx_organizations_tenant_status");
	}

	@Test
	void migrationDoesNotUseDestructiveSql() throws IOException {
		String sql = normalizedMigrationSql().toUpperCase();

		assertThat(sql).doesNotContain("DROP TABLE ORGANIZATIONS");
		assertThat(sql).doesNotContain("TRUNCATE TABLE ORGANIZATIONS");
		assertThat(sql).doesNotContain("DELETE FROM ORGANIZATIONS");
		assertThat(sql).doesNotContain(" CASCADE");
	}

	@Test
	void hardeningMigrationCompletesPartiallyAppliedOrganizationSchema() throws IOException {
		String sql = normalizedSql(HARDENING_MIGRATION);

		assertThat(sql).contains("ALTER COLUMN created_at TYPE TIMESTAMPTZ");
		assertThat(sql).contains("ALTER COLUMN updated_at TYPE TIMESTAMPTZ");
		assertThat(sql).contains("UPDATE organizations");
		assertThat(sql).contains("ALTER COLUMN display_name SET NOT NULL");
		assertThat(sql).contains("chk_organizations_status");
		assertThat(sql).contains("chk_organizations_country_code");
		assertThat(sql).contains("chk_organizations_currency_code");
		assertThat(sql).contains("chk_organizations_contact_email");
		assertThat(sql).contains("chk_organizations_date_format");
		assertThat(sql).contains("chk_organizations_time_format");
		assertThat(sql).contains("chk_organizations_week_start");
		assertThat(sql).contains("chk_organizations_audit_chronology");
		assertThat(sql).contains("chk_organizations_version");
	}

	private static String normalizedMigrationSql() throws IOException {
		return normalizedSql(MIGRATION);
	}

	private static String normalizedSql(Path path) throws IOException {
		return Files.readString(path)
				.replaceAll("\\s+", " ")
				.trim();
	}

	private static int positionOf(String sql, String value) {
		int position = sql.indexOf(value);
		assertThat(position).isNotNegative();
		return position;
	}
}
