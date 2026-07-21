package com.odinsync.organization.infrastructure.persistence.repository;

import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.ORGANIZATION_ID;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.TENANT_ID;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.createdOrganizationWithPendingEvent;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.entity;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.organization;
import static com.odinsync.organization.infrastructure.persistence.OrganizationPersistenceTestFixtures.updateTargetEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.odinsync.organization.domain.model.Organization;
import com.odinsync.organization.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.odinsync.organization.infrastructure.persistence.exception.OrganizationOptimisticLockException;
import com.odinsync.organization.infrastructure.persistence.exception.OrganizationPersistenceException;
import com.odinsync.organization.infrastructure.persistence.mapper.OrganizationPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class OrganizationRepositoryAdapterTest {

	@Mock
	private SpringDataOrganizationRepository repository;

	@Mock
	private OrganizationPersistenceMapper mapper;

	@InjectMocks
	private OrganizationRepositoryAdapter adapter;

	@Test
	void findsOrganizationByTenantIdUsingMapper() {
		OrganizationJpaEntity entity = entity();
		Organization organization = organization();
		when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(entity));
		when(mapper.toDomain(entity)).thenReturn(organization);

		Optional<Organization> result = adapter.findByTenantId(TENANT_ID);

		assertThat(result).containsSame(organization);
		verify(repository).findByTenantId(TENANT_ID);
		verify(mapper).toDomain(entity);
	}

	@Test
	void returnsEmptyWhenTenantScopedOrganizationIsMissing() {
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());

		Optional<Organization> result = adapter.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID);

		assertThat(result).isEmpty();
		verifyNoInteractions(mapper);
	}

	@Test
	void checksOrganizationExistenceByTenantId() {
		when(repository.existsByTenantId(TENANT_ID)).thenReturn(true);

		boolean exists = adapter.existsByTenantId(TENANT_ID);

		assertThat(exists).isTrue();
		verify(repository).existsByTenantId(TENANT_ID);
	}

	@Test
	void insertsNewAggregateWhenEntityDoesNotExist() {
		Organization organization = organization();
		OrganizationJpaEntity newEntity = entity();
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(mapper.toNewEntity(organization)).thenReturn(newEntity);

		adapter.save(organization);

		verify(mapper).toNewEntity(organization);
		verify(mapper, never()).updateEntity(any(), any());
		verify(repository).save(newEntity);
	}

	@Test
	void updatesExistingManagedEntityWhenEntityExists() {
		Organization organization = organization();
		OrganizationJpaEntity existingEntity = updateTargetEntity();
		long originalVersion = existingEntity.getVersion();
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.of(existingEntity));

		adapter.save(organization);

		verify(mapper).updateEntity(organization, existingEntity);
		verify(mapper, never()).toNewEntity(any());
		ArgumentCaptor<OrganizationJpaEntity> savedEntity = ArgumentCaptor.forClass(OrganizationJpaEntity.class);
		verify(repository).save(savedEntity.capture());
		assertThat(savedEntity.getValue()).isSameAs(existingEntity);
		assertThat(savedEntity.getValue().getId()).isEqualTo(ORGANIZATION_ID);
		assertThat(savedEntity.getValue().getTenantId()).isEqualTo(TENANT_ID);
		assertThat(savedEntity.getValue().getVersion()).isEqualTo(originalVersion);
	}

	@Test
	void translatesOptimisticLockFailures() {
		Organization organization = organization();
		OrganizationJpaEntity newEntity = entity();
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(mapper.toNewEntity(organization)).thenReturn(newEntity);
		when(repository.save(newEntity)).thenThrow(new OptimisticLockingFailureException("stale"));

		assertThatThrownBy(() -> adapter.save(organization))
				.isInstanceOf(OrganizationOptimisticLockException.class)
				.hasMessageContaining("modified by another transaction");
	}

	@Test
	void translatesGenericPersistenceFailures() {
		when(repository.findByTenantId(TENANT_ID)).thenThrow(new DataRetrievalFailureException("database unavailable"));

		assertThatThrownBy(() -> adapter.findByTenantId(TENANT_ID))
				.isInstanceOf(OrganizationPersistenceException.class)
				.hasMessageContaining("Unable to load Organization by tenant ID");
	}

	@Test
	void preservesDataIntegrityViolationForSharedConflictHandling() {
		Organization organization = organization();
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID))
				.thenThrow(new DataIntegrityViolationException("unique constraint"));

		assertThatThrownBy(() -> adapter.save(organization))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void doesNotConsumeDomainEventsDuringPersistenceMapping() {
		Organization organization = createdOrganizationWithPendingEvent();
		OrganizationJpaEntity newEntity = entity();
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(mapper.toNewEntity(organization)).thenReturn(newEntity);

		adapter.save(organization);

		assertThat(organization.pullDomainEvents()).hasSize(1);
	}

	@Test
	void rejectsNullInputs() {
		assertThatThrownBy(() -> adapter.findByTenantId(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> adapter.findByIdAndTenantId(null, TENANT_ID)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> adapter.findByIdAndTenantId(ORGANIZATION_ID, null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> adapter.existsByTenantId(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> adapter.save(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void acceptsArbitraryTenantIdsWithoutCrossTenantFallback() {
		UUID otherTenantId = UUID.fromString("99999999-9999-9999-9999-999999999999");
		when(repository.findByIdAndTenantId(ORGANIZATION_ID, otherTenantId)).thenReturn(Optional.empty());

		Optional<Organization> result = adapter.findByIdAndTenantId(ORGANIZATION_ID, otherTenantId);

		assertThat(result).isEmpty();
		verify(repository).findByIdAndTenantId(ORGANIZATION_ID, otherTenantId);
	}
}
