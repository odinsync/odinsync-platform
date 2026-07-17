package com.odinsync.identity.domain.exception;

public class InactiveUserException extends RuntimeException {

	public InactiveUserException() {
		super("User account is not active");
	}
}
