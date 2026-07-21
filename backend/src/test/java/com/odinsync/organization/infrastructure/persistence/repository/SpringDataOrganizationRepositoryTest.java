package com.odinsync.organization.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class SpringDataOrganizationRepositoryTest {

	@Test
	void extendsJpaRepositoryForOrganizationEntity() {
		assertThat(JpaRepository.class).isAssignableFrom(SpringDataOrganizationRepository.class);
	}

	@Test
	void declaresTenantScopedLookupMethods() throws NoSuchMethodException {
		Method findByTenantId = SpringDataOrganizationRepository.class.getMethod("findByTenantId", UUID.class);
		Method findByIdAndTenantId = SpringDataOrganizationRepository.class.getMethod(
				"findByIdAndTenantId",
				UUID.class,
				UUID.class);
		Method existsByTenantId = SpringDataOrganizationRepository.class.getMethod("existsByTenantId", UUID.class);

		assertThat(findByTenantId.getReturnType()).isEqualTo(Optional.class);
		assertThat(findByIdAndTenantId.getReturnType()).isEqualTo(Optional.class);
		assertThat(existsByTenantId.getReturnType()).isEqualTo(boolean.class);
	}

	@Test
	void repositoryMethodsUseOrganizationJpaEntityOptionalContract() throws NoSuchMethodException {
		Method findByTenantId = SpringDataOrganizationRepository.class.getMethod("findByTenantId", UUID.class);
		Method findByIdAndTenantId = SpringDataOrganizationRepository.class.getMethod(
				"findByIdAndTenantId",
				UUID.class,
				UUID.class);

		assertThat(findByTenantId.getGenericReturnType().getTypeName())
				.contains(Optional.class.getName(), OrganizationJpaEntity.class.getName());
		assertThat(findByIdAndTenantId.getGenericReturnType().getTypeName())
				.contains(Optional.class.getName(), OrganizationJpaEntity.class.getName());
	}
}
