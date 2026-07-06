package com.odinsync.identity.application.port.out;

public interface PasswordEncoderPort {

	String encode(String rawPassword);
}
