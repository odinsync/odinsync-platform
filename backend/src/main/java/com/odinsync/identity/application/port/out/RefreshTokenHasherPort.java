package com.odinsync.identity.application.port.out;

public interface RefreshTokenHasherPort {

	String hash(String rawToken);
}
