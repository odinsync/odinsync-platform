package com.odinsync.organization.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.odinsync.organization.domain.model.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

class OrganizationJpaEntityTest {

	@Test
	void rootEntityUsesExpectedJpaMetadata() throws NoSuchFieldException {
		assertThat(OrganizationJpaEntity.class.isAnnotationPresent(Entity.class)).isTrue();
		Entity entity = OrganizationJpaEntity.class.getAnnotation(Entity.class);
		assertThat(entity.name()).isEqualTo("OrganizationAggregateJpaEntity");
		Table table = OrganizationJpaEntity.class.getAnnotation(Table.class);
		assertThat(table.name()).isEqualTo("organizations");
		assertThat(OrganizationJpaEntity.class.getDeclaredField("id").isAnnotationPresent(Id.class)).isTrue();
		assertThat(OrganizationJpaEntity.class.getDeclaredField("id").isAnnotationPresent(GeneratedValue.class)).isFalse();
		assertThat(OrganizationJpaEntity.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

		Column tenantColumn = OrganizationJpaEntity.class.getDeclaredField("tenantId").getAnnotation(Column.class);
		assertThat(tenantColumn.name()).isEqualTo("tenant_id");
		assertThat(tenantColumn.nullable()).isFalse();
		assertThat(tenantColumn.updatable()).isFalse();

		Enumerated statusMapping = OrganizationJpaEntity.class.getDeclaredField("status")
				.getAnnotation(Enumerated.class);
		assertThat(statusMapping.value()).isEqualTo(EnumType.STRING);
	}

	@Test
	void embeddablesUseExpectedJpaMetadata() {
		assertThat(OrganizationAddressEmbeddable.class.isAnnotationPresent(Embeddable.class)).isTrue();
		assertThat(OrganizationContactEmbeddable.class.isAnnotationPresent(Embeddable.class)).isTrue();
		assertThat(OrganizationSettingsEmbeddable.class.isAnnotationPresent(Embeddable.class)).isTrue();
		assertThat(OrganizationAuditEmbeddable.class.isAnnotationPresent(Embeddable.class)).isTrue();
	}

	@Test
	void settingsEnumsUseStringMapping() throws NoSuchFieldException {
		assertThat(enumMapping("dateFormat").value()).isEqualTo(EnumType.STRING);
		assertThat(enumMapping("timeFormat").value()).isEqualTo(EnumType.STRING);
		assertThat(enumMapping("weekStart").value()).isEqualTo(EnumType.STRING);
	}

	@Test
	void entityContainsNoBusinessBehavior() {
		assertThat(OrganizationJpaEntity.class.getDeclaredMethods())
				.noneMatch(method -> method.getName().equals("updateProfile"))
				.noneMatch(method -> method.getName().equals("updateSettings"))
				.noneMatch(method -> method.getName().equals("activate"))
				.noneMatch(method -> method.getName().equals("suspend"))
				.noneMatch(method -> method.getName().equals("archive"))
				.noneMatch(method -> method.getName().equals("pullDomainEvents"));
	}

	@Test
	void statusCanRepresentLifecycleStates() {
		OrganizationJpaEntity entity = new OrganizationJpaEntity();
		entity.setStatus(OrganizationStatus.SUSPENDED);
		assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
	}

	private Enumerated enumMapping(String fieldName) throws NoSuchFieldException {
		return OrganizationSettingsEmbeddable.class.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
	}
}
