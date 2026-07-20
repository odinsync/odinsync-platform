package com.odinsync.organization.application.exception;

public class MissingActorContextException extends RuntimeException {

	public MissingActorContextException() {
		super("Authenticated actor context is missing");
	}
}
