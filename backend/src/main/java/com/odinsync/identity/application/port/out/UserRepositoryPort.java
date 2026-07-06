package com.odinsync.identity.application.port.out;

import java.util.UUID;

import com.odinsync.identity.domain.model.User;

public interface UserRepositoryPort {

	boolean existsByEmail(String email);

	User save(User user);
}
