package com.odinsync.identity.infrastructure.persistence.adapter;

import com.odinsync.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.odinsync.identity.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.odinsync.identity.application.port.out.UserRepositoryPort;
import com.odinsync.identity.domain.model.User;
import com.odinsync.identity.infrastructure.persistence.mapper.UserPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

	private final UserJpaRepository repository;
	private final UserPersistenceMapper mapper;

	@Override
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	@Override
	public User save(User user) {
		UserJpaEntity saved = repository.save(mapper.toEntity(user));
		return mapper.toDomain(saved);
	}
}
