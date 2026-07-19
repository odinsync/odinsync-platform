package com.odinsync.identity.domain.exception;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Refresh token is invalid or expired");
	}
}
